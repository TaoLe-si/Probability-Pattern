/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.part;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.tz.statpatterns.ProbabilityPatternMod;

import appeng.helpers.Reflected;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.util.Platform;

/**
 * Cable-attached ME Probability Pattern Encoding Terminal.
 * <p>
 * Extends AE2's {@link PartPatternTerminal} (wired pattern terminal part) and adds the
 * probability parameters (single-attempt success probability p and the alpha95/alpha99
 * confidence flag). These are persisted in the part NBT and are baked into every
 * encoded probability pattern.
 */
public class ProbabilityPatternTerminalPart extends PartPatternTerminal {

    private static final String NBT_PROBABILITY = "probability";
    private static final String NBT_ALPHA95 = "alpha95";

    private double probability = 0.8;
    private boolean alpha95 = true;

    @Reflected
    public ProbabilityPatternTerminalPart(final ItemStack is) {
        super(is);
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey(NBT_PROBABILITY)) {
            this.probability = MathHelper.clamp_double(data.getDouble(NBT_PROBABILITY), 0.01, 0.9999);
        }
        if (data.hasKey(NBT_ALPHA95)) {
            this.alpha95 = data.getBoolean(NBT_ALPHA95);
        }
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        data.setDouble(NBT_PROBABILITY, this.probability);
        data.setBoolean(NBT_ALPHA95, this.alpha95);
    }

    public double getProbability() {
        return this.probability;
    }

    public boolean isAlpha95() {
        return this.alpha95;
    }

    public void setProbability(final double probability) {
        this.probability = MathHelper.clamp_double(probability, 0.01, 0.9999);
        this.saveChanges();
    }

    public void setAlpha95(final boolean alpha95) {
        this.alpha95 = alpha95;
        this.saveChanges();
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final Vec3 pos) {
        if (Platform.isClient()) {
            return true;
        }

        if (player.isSneaking()) {
            return false;
        }

        final ForgeDirection side = this.getSide();
        final int x = this.getTile().xCoord;
        final int y = this.getTile().yCoord;
        final int z = this.getTile().zCoord;
        // Encode the part side in the upper 3 bits of the gui id so the gui handler
        // can resolve the exact part on the cable bus.
        player.openGui(ProbabilityPatternMod.instance, side.ordinal(), player.worldObj, x, y, z);

        return true;
    }
}
