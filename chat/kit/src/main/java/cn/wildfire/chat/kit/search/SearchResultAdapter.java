package cn.wildfire.chat.kit.search;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.search.viewHolder.CategoryViewHolder;
import cn.wildfire.chat.kit.search.viewHolder.ExpandViewHolder;
import cn.wildfirechat.chat.R;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_CATEGORY = 0;
    private static final int VIEW_TYPE_EXPAND = 1;
    private List<InternalSearchResult> results = new ArrayList<>();
    private SparseArray<SearchableModule> viewTypeToSearchModule = new SparseArray<>();
    private Fragment fragment;

    public SearchResultAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public void submitSearResult(SearchResult result) {
        if (result.result == null || result.result.isEmpty()) {
            return;
        }
        InternalSearchResult ir = new InternalSearchResult(result);
        int index = 0;
        index = findIndex(ir);
        if (index < results.size()) {
            results.add(index, ir);
        } else {
            results.add(ir);
        }
        if (index > 0) {
            InternalSearchResult preIr = results.get(index - 1);
            ir.startPosition = preIr.endPosition + 1;
        } else {
            ir.startPosition = 0;
        }
        if (ir.module.expandable() && ir.results.size() > SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT) {
            ir.endPosition = ir.startPosition + SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT + 1;
        } else {
            ir.endPosition = ir.startPosition + ir.results.size();
        }
        int newItemCount = ir.endPosition - ir.startPosition + 1;
        InternalSearchResult next;
        for (int i = index + 1; i < results.size(); i++) {
            next = results.get(i);
            next.startPosition = next.startPosition + newItemCount;
            next.endPosition = next.endPosition + newItemCount;
        }
        notifyItemRangeInserted(ir.startPosition, newItemCount);
    }

    public void reset() {
        results.clear();
        viewTypeToSearchModule.clear();
        notifyDataSetChanged();
    }

    public void expandModuleResult(SearchableModule module) {
        InternalSearchResult ir;
        int targetIndex = 0;
        for (int i = 0; i < results.size(); i++) {
            ir = results.get(i);
            if (ir.module == module) {
                int preEndPosition = ir.endPosition;
                ir.endPosition = ir.startPosition + ir.results.size();
                ir.isExpanded = true;
                // 其实这儿可以做收起功能
                notifyItemChanged(preEndPosition);
                if (ir.endPosition - preEndPosition > 1) {
                    notifyItemRangeInserted(preEndPosition + 1, ir.endPosition - preEndPosition);
                }
                targetIndex = i;
                break;
            }
        }

        for (int i = targetIndex + 1; i < results.size(); i++) {
            ir = results.get(i);
            ir.startPosition = ir.startPosition + results.get(targetIndex).results.size() - SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT - 1;
            ir.endPosition = ir.endPosition + results.get(targetIndex).results.size() - SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT - 1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_CATEGORY) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item_category, parent, false);
            return new CategoryViewHolder(view);
        } else if (viewType == VIEW_TYPE_EXPAND) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item_expand, parent, false);
            RecyclerView.ViewHolder holder = new ExpandViewHolder(view);
            processOnClick(holder, view);
            return holder;
        } else {
            SearchableModule model = viewTypeToSearchModule.get(viewType);
            RecyclerView.ViewHolder holder = model.onCreateViewHolder(fragment, parent, viewType);
            processOnClick(holder, holder.itemView);
            return holder;
        }
    }

    private void processOnClick(RecyclerView.ViewHolder holder, View view) {
        view.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            InternalSearchResult ir = getInternalResultAtPosition(position);
            if (holder instanceof ExpandViewHolder) {
                expandModuleResult(ir.module);
            } else {
                ir.module.onClickInternal(fragment, holder, v, getResultItemAtPosition(position));
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        InternalSearchResult ir = getInternalResultAtPosition(position);
        switch (type) {
            case VIEW_TYPE_CATEGORY:
                ((CategoryViewHolder) holder).onBind(ir.module.category());
                break;
            case VIEW_TYPE_EXPAND:
                ((ExpandViewHolder) holder).onBind(ir.module.category(), ir.results.size() - SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT);
                break;
            default:
                ir.module.onBind(fragment, holder, getResultItemAtPosition(position));
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (results == null || results.isEmpty()) {
            return 0;
        } else {
            int count = 0;
            for (InternalSearchResult ir : results) {
                if (ir.module.expandable() && ir.results.size() > SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT && !ir.isExpanded) {
                    count += 1 + SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT + 1;
                } else {
                    count += 1 + ir.results.size();
                }
            }
            return count;
        }
    }

    private Object getResultItemAtPosition(int position) {
        InternalSearchResult ir = getInternalResultAtPosition(position);
        return ir.results.get(position - ir.startPosition - 1);
    }

    private InternalSearchResult getInternalResultAtPosition(int position) {
        InternalSearchResult result = null;
        for (InternalSearchResult ir : results) {
            if (position == ir.startPosition) {
                result = ir;
                break;
            } else {
                if (ir.module.expandable() && !ir.isExpanded && ir.results.size() > SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT) {
                    if (position <= ir.startPosition + SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT + 1) {
                        result = ir;
                        break;
                    } else {
                        // continue
                    }
                } else {
                    if (position <= ir.endPosition) {
                        result = ir;
                        break;
                    } else {
                        // continue
                    }
                }
            }
        }

        return result;
    }

    /**
     * @param position
     * @return -1, error; 0, category; 1, expand; > 10000 具体搜索结果类型
     */
    @Override
    public int getItemViewType(int position) {
        int type = -1;
        SearchableModule preModule;
        SearchableModule module = null;
        for (InternalSearchResult ir : results) {
            if (position == ir.startPosition) {
                type = VIEW_TYPE_CATEGORY;
                break;
            } else {
                if (ir.module.expandable() && !ir.isExpanded && ir.results.size() > SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT) {
                    if (position <= ir.startPosition + SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT) {
                        type = ir.module.getViewType(ir.results.get(position - ir.startPosition - 1));
                        module = ir.module;
                        break;
                    } else if (position == ir.startPosition + SearchableModule.DEFAULT_SHOW_RESULT_ITEM_COUNT + 1) {
                        // expand
                        module = ir.module;
                        type = VIEW_TYPE_EXPAND;
                        break;
                    } else {
                        // continue
                    }
                } else {
                    if (position <= ir.endPosition) {
                        type = ir.module.getViewType(ir.results.get(position - ir.startPosition - 1));
                        module = ir.module;
                        break;
                    } else {
                        // continue
                    }
                }
            }
        }
        if (type != VIEW_TYPE_CATEGORY && type != VIEW_TYPE_EXPAND && type != -1) {
            preModule = viewTypeToSearchModule.get(type);
            if (preModule != null && preModule != module) {
                throw new RuntimeException("duplicate search module view type");
            }
            viewTypeToSearchModule.put(type, module);
        }
        return type;
    }

    private int findIndex(InternalSearchResult result) {
        if (results == null || results.isEmpty()) {
            return 0;
        }
        int i = 0;
        for (; i < results.size(); i++) {
            if (result.module.priority() > results.get(i).module.priority()) {
                break;
            } else {
                // do nothing
            }
        }
        return i;
    }

    private static class InternalSearchResult {
        SearchableModule module;
        // start, end 都是闭区间[]
        int startPosition;
        int endPosition;
        // only valid for expandable search module
        boolean isExpanded = false;
        List<Object> results;


        public InternalSearchResult(SearchResult result) {
            this.module = result.module;
            this.results = result.result;
        }
    }
}
