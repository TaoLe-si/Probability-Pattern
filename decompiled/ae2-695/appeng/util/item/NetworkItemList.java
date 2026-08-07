/*
 * Decompiled with CFR 0.152.
 */
package appeng.util.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMENetworkInventory;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NetworkItemList<T extends IAEStack>
implements IItemList<T> {
    private final IMENetworkInventory<T> network;
    private final Map<IMENetworkInventory<T>, IItemList<T>> networkItemLists;
    private final Supplier<IItemList<T>> newItemListSupplier;
    private final List<Predicate<T>> predicates;

    public NetworkItemList(IMENetworkInventory<T> network, Supplier<IItemList<T>> newItemListSupplier) {
        this.network = network;
        this.networkItemLists = new HashMap<IMENetworkInventory<T>, IItemList<T>>();
        this.predicates = new ArrayList<Predicate<T>>();
        this.newItemListSupplier = newItemListSupplier;
    }

    public NetworkItemList(NetworkItemList<T> networkItemList) {
        this.network = networkItemList.network;
        this.predicates = new ArrayList<Predicate<T>>();
        this.networkItemLists = networkItemList.networkItemLists;
        this.newItemListSupplier = networkItemList.newItemListSupplier;
    }

    public void addNetworkItems(IMENetworkInventory<T> network, IItemList<T> itemList) {
        IItemList<T> l = this.networkItemLists.get(network);
        if (l instanceof NetworkItemList) {
            if (itemList instanceof NetworkItemList) {
                for (Predicate<T> filter : ((NetworkItemList)itemList).predicates) {
                    ((NetworkItemList)l).addFilter(filter);
                }
                return;
            }
            throw new RuntimeException("This NetworkItemList already contains a NetworkItemList for the provided network and cannot replace it with a non-NetworkItemList");
        }
        if (l != null) {
            throw new RuntimeException("This NetworkItemList already contains a non-NetworkItemList for the provided network and cannot replace it");
        }
        this.networkItemLists.put(network, itemList);
    }

    private Stream<NetworkItemStack<T>> getNetworkItemStackStream(Set<IMENetworkInventory<T>> visitedNetworks) {
        return this.networkItemLists.entrySet().stream().flatMap(entry -> {
            if (entry.getValue() instanceof NetworkItemList) {
                if (visitedNetworks.contains(entry.getKey())) {
                    return Stream.empty();
                }
                HashSet<IMENetworkInventory> localVisitedNetworks = new HashSet<IMENetworkInventory>(visitedNetworks);
                localVisitedNetworks.add((IMENetworkInventory)entry.getKey());
                return ((NetworkItemList)entry.getValue()).getFilteredNetworkItemStackStream(Collections.unmodifiableSet(localVisitedNetworks));
            }
            ArrayList buffer = new ArrayList();
            StreamSupport.stream(((IItemList)entry.getValue()).spliterator(), false).forEach(item -> buffer.add(new NetworkItemStack(this, (IMENetworkInventory)entry.getKey(), item)));
            return buffer.stream();
        });
    }

    private Stream<NetworkItemStack<T>> filter(Stream<NetworkItemStack<T>> stream) {
        Predicate predicate = this.buildFilter();
        if (predicate == null) {
            return stream;
        }
        return stream.filter((? super T e) -> predicate.test(e.getItemStack()));
    }

    public Stream<T> getItems() {
        return this.getFilteredNetworkItemStackStream().distinct().map(NetworkItemStack::getItemStack);
    }

    private Stream<NetworkItemStack<T>> getFilteredNetworkItemStackStream() {
        HashSet<IMENetworkInventory<T>> visitedNetworks = new HashSet<IMENetworkInventory<T>>();
        visitedNetworks.add(this.network);
        return this.filter(this.getNetworkItemStackStream(Collections.unmodifiableSet(visitedNetworks)));
    }

    private Stream<NetworkItemStack<T>> getFilteredNetworkItemStackStream(Set<IMENetworkInventory<T>> visitedNetworks) {
        return this.filter(this.getNetworkItemStackStream(visitedNetworks));
    }

    public void addFilter(Predicate<T> filter) {
        this.predicates.add(filter);
    }

    private Predicate<T> buildFilter() {
        Predicate<T> predicate = null;
        for (Predicate<T> filter : this.predicates) {
            if (predicate == null) {
                predicate = filter;
                continue;
            }
            predicate = predicate.or(filter);
        }
        return predicate;
    }

    public IItemList<T> buildFinalItemList() {
        IItemList<T> out = this.newItemListSupplier.get();
        return this.buildFinalItemList(out);
    }

    public IItemList<T> buildFinalItemList(IItemList<T> out) {
        this.getItems().forEach(out::add);
        return out;
    }

    @Override
    public void addStorage(T option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCrafting(T option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRequestable(T option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T getFirstItem() {
        return (T)((IAEStack)this.getItems().findFirst().orElse(null));
    }

    @Override
    public int size() {
        return (int)this.getItems().count();
    }

    @Override
    public Iterator<T> iterator() {
        return this.getItems().iterator();
    }

    @Override
    public void resetStatus() {
        for (IItemList<T> list : this.networkItemLists.values()) {
            list.resetStatus();
        }
    }

    @Override
    public void add(T option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T findPrecise(T i) {
        return this.buildFinalItemList().findPrecise(i);
    }

    @Override
    public Collection<T> findFuzzy(T input, FuzzyMode fuzzy) {
        return this.buildFinalItemList().findFuzzy(input, fuzzy);
    }

    @Override
    public boolean isEmpty() {
        return !this.getItems().findAny().isPresent();
    }

    private static class NetworkItemStack<U extends IAEStack> {
        private final IMENetworkInventory<U> networkInventory;
        private final U itemStack;
        final /* synthetic */ NetworkItemList this$0;

        public NetworkItemStack(IMENetworkInventory<U> networkInventory, U itemStack) {
            this.this$0 = var1_1;
            this.networkInventory = networkInventory;
            this.itemStack = itemStack;
        }

        public IMENetworkInventory<U> getNetworkInventory() {
            return this.networkInventory;
        }

        public U getItemStack() {
            return this.itemStack;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NetworkItemStack)) {
                return false;
            }
            NetworkItemStack that = (NetworkItemStack)o;
            return this.networkInventory == that.networkInventory && this.itemStack == that.itemStack;
        }

        public int hashCode() {
            int prime = 31;
            int hash = 1;
            hash = hash * 31 + System.identityHashCode(this.networkInventory);
            hash = hash * 31 + System.identityHashCode(this.itemStack);
            return hash;
        }
    }
}

