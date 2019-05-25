public class Position {
    int r,c;
    public Position(int r, int c){
        this.r = r;
        this.c = c;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        return "Row: " + r + " Col: " + c;
    }
}
