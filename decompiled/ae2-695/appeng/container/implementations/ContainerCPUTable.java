/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  net.minecraft.entity.player.EntityPlayerMP
 */
package appeng.container.implementations;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.container.interfaces.ICraftingCPUSelectorContainer;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCraftingCPUsUpdate;
import appeng.util.Platform;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.entity.player.EntityPlayerMP;

public class ContainerCPUTable
implements ICraftingCPUSelectorContainer {
    private final AEBaseContainer parent;
    private ImmutableSet<ICraftingCPU> lastCpuSet = null;
    private List<CraftingCPUStatus> cpus = new ArrayList<CraftingCPUStatus>();
    private final WeakHashMap<ICraftingCPU, Integer> cpuSerialMap = new WeakHashMap();
    private int nextCpuSerial = 1;
    private int lastUpdate = 0;
    @GuiSync(value=0)
    public int selectedCpuSerial = -1;
    private final Consumer<ICraftingCPU> onCPUChange;
    private final boolean preferBusyCPUs;
    private final Predicate<CraftingCPUStatus> cpuFilter;
    private static final Comparator<CraftingCPUStatus> CPU_COMPARATOR = Comparator.comparing(e -> e.getName() == null || e.getName().isEmpty()).thenComparing(e -> e.getName() != null ? e.getName() : "").thenComparingInt(CraftingCPUStatus::getSerial);

    public ContainerCPUTable(AEBaseContainer parent, Consumer<ICraftingCPU> onCPUChange, boolean preferBusyCPUs, Predicate<CraftingCPUStatus> cpuFilter) {
        this.parent = parent;
        this.onCPUChange = onCPUChange;
        this.preferBusyCPUs = preferBusyCPUs;
        this.cpuFilter = cpuFilter;
    }

    public boolean isBusyCPUsPreferred() {
        return this.preferBusyCPUs;
    }

    public Predicate<CraftingCPUStatus> getCpuFilter() {
        return this.cpuFilter;
    }

    public void detectAndSendChanges(IGrid network, List<?> crafters) {
        if (Platform.isServer() && network != null) {
            ICraftingGrid cc = (ICraftingGrid)network.getCache(ICraftingGrid.class);
            ImmutableSet<ICraftingCPU> cpuSet = cc.getCpus();
            ++this.lastUpdate;
            if (!cpuSet.equals(this.lastCpuSet) || this.lastUpdate > 20) {
                this.lastUpdate = 0;
                this.lastCpuSet = cpuSet;
                this.updateCpuList();
                this.sendCPUs(crafters);
            }
        }
        if (this.selectedCpuSerial != -1 && this.cpus.stream().noneMatch(c -> c.getSerial() == this.selectedCpuSerial)) {
            this.selectCPU(-1);
        }
        if (this.selectedCpuSerial == -1) {
            if (!this.preferBusyCPUs) {
                for (CraftingCPUStatus c2 : this.cpus) {
                    if (!c2.isBusy() || !this.cpuFilter.test(c2)) continue;
                    this.selectCPU(c2.getSerial());
                    return;
                }
            }
            for (CraftingCPUStatus cpu : this.cpus) {
                if (this.preferBusyCPUs != cpu.isBusy() || !this.cpuFilter.test(cpu)) continue;
                this.selectCPU(cpu.getSerial());
                break;
            }
            if (this.selectedCpuSerial == -1 && !this.cpus.isEmpty()) {
                this.selectCPU(this.cpus.get(0).getSerial());
            }
        }
    }

    private void updateCpuList() {
        this.cpus.clear();
        for (ICraftingCPU cpu : this.lastCpuSet) {
            int serial = this.getOrAssignCpuSerial(cpu);
            this.cpus.add(new CraftingCPUStatus(cpu, serial));
        }
        this.cpus.sort(CPU_COMPARATOR);
    }

    private int getOrAssignCpuSerial(ICraftingCPU cpu) {
        return this.cpuSerialMap.computeIfAbsent(cpu, unused -> this.nextCpuSerial++);
    }

    private void sendCPUs(List<?> crafters) {
        for (Object player : crafters) {
            if (!(player instanceof EntityPlayerMP)) continue;
            try {
                NetworkHandler.instance.sendTo(new PacketCraftingCPUsUpdate(this.cpus), (EntityPlayerMP)player);
            }
            catch (IOException e) {
                AELog.debug(e);
            }
        }
    }

    @Override
    public void selectCPU(int serial) {
        if (Platform.isServer()) {
            if (serial < -1) {
                serial = -1;
            }
            int searchedSerial = serial;
            if (serial > -1 && this.cpus.stream().noneMatch(c -> c.getSerial() == searchedSerial)) {
                serial = -1;
            }
            ICraftingCPU newSelectedCpu = null;
            if (serial != -1) {
                for (ICraftingCPU cpu : this.lastCpuSet) {
                    if (this.cpuSerialMap.getOrDefault(cpu, -1) != serial) continue;
                    newSelectedCpu = cpu;
                    break;
                }
            }
            this.selectedCpuSerial = serial;
            if (this.onCPUChange != null) {
                this.onCPUChange.accept(newSelectedCpu);
            }
        }
    }

    public List<CraftingCPUStatus> getCPUs() {
        return Collections.unmodifiableList(this.cpus);
    }

    public CraftingCPUStatus getSelectedCPU() {
        return this.cpus.stream().filter(c -> c.getSerial() == this.selectedCpuSerial).findFirst().orElse(null);
    }

    public void handleCPUUpdate(CraftingCPUStatus[] cpus) {
        this.cpus = Arrays.asList(cpus);
    }
}

