package ch.rakudave.jnetmap.util;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("Tuple")
public class Tuple<G, H> {
    private G first;
    private H second;

    /**
     * Defines a simple return-type that can hold two values
     *
     * @param first
     * @param second
     */
    public Tuple(G first, H second) {
        this.first = first;
        this.second = second;
    }

    public Tuple() {
    }

    public G getFirst() {
        return first;
    }

    public H getSecond() {
        return second;
    }

    public void setFirst(G first) {
        this.first = first;
    }

    public void setSecond(H second) {
        this.second = second;
    }
}
