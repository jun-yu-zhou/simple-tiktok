package com.example.simpletiktok.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class TopK<T> {

    private final int k;
    private final PriorityQueue<T> queue;

    public TopK(int k, PriorityQueue<T> queue) {
        this.k = k;
        this.queue = queue;
    }

    public void add(T value) {
        if (queue.size() < k) {
            queue.add(value);
            return;
        }
        if (queue.peek() == null || queue.comparator() == null) {
            return;
        }
        if (queue.comparator().compare(value, queue.peek()) > 0) {
            queue.poll();
            queue.add(value);
        }
    }

    public List<T> getDescList() {
        List<T> list = new ArrayList<>(queue);
        if (queue.comparator() != null) {
            list.sort(queue.comparator().reversed());
        }
        return list;
    }
}
