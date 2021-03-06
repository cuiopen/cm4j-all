package com.cm4j.test.guava.consist.fifo;

import com.google.common.collect.AbstractSequentialIterator;

import java.util.AbstractQueue;
import java.util.Iterator;

/**
 * FIFO队列，同一对象可自动添加到队列尾部
 *
 * <font color="red">非线程安全</font>
 *
 * Created by yanghao on 14-3-26.
 */
public class FIFOAccessQueue<E extends IQueueEntry> extends AbstractQueue<E> {

    private final IQueueEntry head = new IQueueEntry(){
        private IQueueEntry nextAccess = this, previousAccess = this;
        @Override
        public IQueueEntry getNextInAccessQueue() {
            return nextAccess;
        }

        @Override
        public void setNextInAccessQueue(IQueueEntry next) {
            this.nextAccess = next;
        }

        @Override
        public IQueueEntry getPreviousInAccessQueue() {
            return previousAccess;
        }

        @Override
        public void setPreviousInAccessQueue(IQueueEntry previous) {
            this.previousAccess = previous;
        }

        @Override
        public String toString() {
            return "FIFO_ENTRY_HEAD";
        }
    };

    @Override
    public boolean offer(E entry) {
        // unlink
        connectAccessOrder(entry.getPreviousInAccessQueue(), entry.getNextInAccessQueue());

        // add to tail
        connectAccessOrder(head.getPreviousInAccessQueue(), entry);
        connectAccessOrder(entry, head);

        return true;
    }

    @Override
    public E peek() {
        IQueueEntry next = head.getNextInAccessQueue();
        return (next == head) ? null : (E) next;
    }

    @Override
    public E poll() {
        IQueueEntry next = head.getNextInAccessQueue();
        if (next == head) {
            return null;
        }

        remove(next);
        return (E) next;
    }

    @Override
    public boolean remove(Object o) {
        IQueueEntry e = (IQueueEntry) o;
        IQueueEntry previous = e.getPreviousInAccessQueue();
        IQueueEntry next = e.getNextInAccessQueue();
        connectAccessOrder(previous, next);
        nullifyAccessOrder(e);

        return next != NullEntry.INSTANCE;
    }

    @Override
    public boolean contains(Object o) {
        E e = (E) o;
        return e.getNextInAccessQueue() != NullEntry.INSTANCE;
    }

    @Override
    public boolean isEmpty() {
        return head.getNextInAccessQueue() == head;
    }

    @Override
    public int size() {
        // TODO 这里应该用个变量记录size大小？
        int size = 0;
        for (IQueueEntry e = head.getNextInAccessQueue(); e != head; e = e.getNextInAccessQueue()) {
            size++;
        }
        return size;
    }

    @Override
    public void clear() {
        IQueueEntry e = head.getNextInAccessQueue();
        while (e != head) {
            IQueueEntry next = e.getNextInAccessQueue();
            nullifyAccessOrder(e);
            e = next;
        }

        head.setNextInAccessQueue(head);
        head.setPreviousInAccessQueue(head);
    }

    @Override
    public Iterator<E> iterator() {
        return new AbstractSequentialIterator<E>(peek()) {
            @Override
            protected E computeNext(E previous) {
                IQueueEntry next = previous.getNextInAccessQueue();
                return (next == head) ? null : (E) next;
            }
        };
    }

    public void connectAccessOrder(IQueueEntry previous, IQueueEntry next) {
        previous.setNextInAccessQueue(next);
        next.setPreviousInAccessQueue(previous);
    }

    public void nullifyAccessOrder(IQueueEntry nulled) {
        NullEntry nullEntry = NullEntry.INSTANCE;
        nulled.setNextInAccessQueue(nullEntry);
        nulled.setPreviousInAccessQueue(nullEntry);
    }

    public IQueueEntry getHead() {
        return head;
    }

    public static void main(String[] args) {
        FIFOAccessQueue<FIFOEntry> queue = new FIFOAccessQueue();
        FIFOEntry a = new FIFOEntry("A") {
        };
        FIFOEntry b = new FIFOEntry("B") {
        };
        FIFOEntry c = new FIFOEntry("C") {
        };
        FIFOEntry d = new FIFOEntry("D") {
        };
        queue.offer(a);
        queue.offer(b);
        queue.offer(c);

        queue.offer(b);

        System.out.println(queue.remove(d));
    }
}
