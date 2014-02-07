package kc.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MultiUserQueue<T> {

	List<Queue<T>> endpoints = new LinkedList<Queue<T>>();

	public Queue<T> subscribe() {
		Queue<T> q = new LinkedList<T>();
		endpoints.add(q);
		return q;
	}

	public void publish(T item) {
		for (Queue<T> q : endpoints) {
			q.add(item);
		}
	}

}
