package br.com.matheus.stockcontrol.store;

import br.com.matheus.stockcontrol.dao.CategoryDao;
import br.com.matheus.stockcontrol.model.Category;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CategoryStore {

    public interface Listener {
        void onCategoriesChanged(List<Category> categories);
    }

    private static final CategoryStore INSTANCE = new CategoryStore();

    public static CategoryStore getInstance() {
        return INSTANCE;
    }

    private final CategoryDao dao = new CategoryDao();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile List<Category> cache = List.of();

    private CategoryStore() {
        // vazio
    }

    public List<Category> getCategories() {
        if (cache == null || cache.isEmpty()) {
            reload();
        }
        return cache;
    }

    public void reload() {
        cache = dao.findAll();
        notifyListeners();
    }

    public void addListener(Listener l) {
        if (l == null) return;
        listeners.addIfAbsent(l);
        // já manda estado atual
        l.onCategoriesChanged(cache == null ? List.of() : cache);
    }

    public void removeListener(Listener l) {
        if (l == null) return;
        listeners.remove(l);
    }

    private void notifyListeners() {
        List<Category> snapshot = cache == null ? List.of() : cache;
        for (Listener l : listeners) {
            l.onCategoriesChanged(snapshot);
        }
    }
}