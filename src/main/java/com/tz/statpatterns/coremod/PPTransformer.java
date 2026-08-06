/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.coremod;

import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.IClassTransformer;

/**
 * ASM transformer that makes AE2's crafting tree run probability patterns enough
 * times.
 * <p>
 * Injects, at the head of
 * {@code appeng.crafting.CraftingTreeProcess.getTimes(long, long)}:
 * <pre>
 *   if (this.details instanceof StatisticalPatternDetails
 *       && ((StatisticalPatternDetails) this.details).isProbabilityPattern()) {
 *       return ((StatisticalPatternDetails) this.details).plannedAttempts(remaining);
 *   }
 * </pre>
 * <p>
 * {@code getTimes(remaining, stackSize)} is exactly where AE2 decides how many
 * times to run a processing pattern to produce {@code remaining} output. For a
 * probability pattern we replace the deterministic {@code ceil(remaining/output)}
 * with the binomial / normal-approximation attempt plan from
 * {@code ProbabilitySizing}, guaranteeing P(produced >= remaining) >= 1 - alpha.
 */
public class PPTransformer implements IClassTransformer
{
	private static final String TARGET_CLASS = "appeng.crafting.CraftingTreeProcess";
	private static final String TARGET_METHOD = "getTimes";
	private static final String TARGET_DESC = "(JJ)J";
	private static final String DETAILS_FIELD = "details";
	private static final String DETAILS_FIELD_DESC = "Lappeng/api/networking/crafting/ICraftingPatternDetails;";
	private static final String SPD_CLASS = "com/tz/statpatterns/crafting/StatisticalPatternDetails";
	private static final String SPD_METHOD_IS_PROB = "isProbabilityPattern";
	private static final String SPD_METHOD_PLANNED = "plannedAttempts";

	@Override
	public byte[] transform( final String name, final String transformedName, final byte[] basicClass )
	{
		// Match either the deobfuscated or runtime (SRG) name.
		if( !TARGET_CLASS.equals( transformedName ) && !TARGET_CLASS.equals( name ) )
		{
			return basicClass;
		}

		try
		{
			// FML's runtime deobfuscator runs before us, so the class uses MCP names;
			// map defensively in case the environment hands us SRG names.
			final String fieldName = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName( TARGET_CLASS, DETAILS_FIELD, DETAILS_FIELD_DESC );

			final ClassReader cr = new ClassReader( basicClass );
			final ClassWriter cw = new ClassWriter( cr, ClassWriter.COMPUTE_MAXS );
			final ClassVisitor cv = new ClassVisitor( Opcodes.ASM4, cw )
			{
				@Override
				public MethodVisitor visitMethod( final int access, final String methodName, final String desc, final String signature, final String[] exceptions )
				{
					final MethodVisitor mv = super.visitMethod( access, methodName, desc, signature, exceptions );
					if( TARGET_METHOD.equals( methodName ) && TARGET_DESC.equals( desc ) )
					{
						return new GetTimesInjector( mv, fieldName );
					}
					return mv;
				}
			};
			cr.accept( cv, 0 );

			FMLRelaunchLog.info( "[ProbabilityPattern] Transformed CraftingTreeProcess.getTimes" );
			return cw.toByteArray();
		}
		catch( final Throwable t )
		{
			FMLRelaunchLog.log( Level.ERROR, t, "[ProbabilityPattern] Failed to transform CraftingTreeProcess.getTimes" );
			return basicClass;
		}
	}

	/**
	 * Injects the probability sizing branch at the head of the method.
	 */
	private static final class GetTimesInjector extends MethodVisitor
	{
		private final String fieldName;

		GetTimesInjector( final MethodVisitor mv, final String fieldName )
		{
			super( Opcodes.ASM4, mv );
			this.fieldName = fieldName;
		}

		@Override
		public void visitCode()
		{
			super.visitCode();

			final Label skip = new Label();

			// if (!(this.details instanceof StatisticalPatternDetails)) goto skip;
			mv.visitVarInsn( Opcodes.ALOAD, 0 );
			mv.visitFieldInsn( Opcodes.GETFIELD, TARGET_CLASS, this.fieldName, DETAILS_FIELD_DESC );
			mv.visitTypeInsn( Opcodes.INSTANCEOF, SPD_CLASS );
			mv.visitJumpInsn( Opcodes.IFEQ, skip );

			// if (!((StatisticalPatternDetails) this.details).isProbabilityPattern()) goto skip;
			mv.visitVarInsn( Opcodes.ALOAD, 0 );
			mv.visitFieldInsn( Opcodes.GETFIELD, TARGET_CLASS, this.fieldName, DETAILS_FIELD_DESC );
			mv.visitTypeInsn( Opcodes.CHECKCAST, SPD_CLASS );
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, SPD_CLASS, SPD_METHOD_IS_PROB, "()Z" );
			mv.visitJumpInsn( Opcodes.IFEQ, skip );

			// return ((StatisticalPatternDetails) this.details).plannedAttempts(remaining);
			mv.visitVarInsn( Opcodes.ALOAD, 0 );
			mv.visitFieldInsn( Opcodes.GETFIELD, TARGET_CLASS, this.fieldName, DETAILS_FIELD_DESC );
			mv.visitTypeInsn( Opcodes.CHECKCAST, SPD_CLASS );
			mv.visitVarInsn( Opcodes.LLOAD, 1 );
			mv.visitMethodInsn( Opcodes.INVOKEVIRTUAL, SPD_CLASS, SPD_METHOD_PLANNED, "(J)J" );
			mv.visitInsn( Opcodes.LRETURN );

			mv.visitLabel( skip );
		}
	}
}
