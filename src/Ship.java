import java.util.ArrayList;
import java.util.List;

public class Ship {
    enum Direction{
        HORIZONTAL, VERTICAL
    }
    List<Position> positions;
    boolean exists;

    public Ship(Position start, int size, Direction direction){
        positions = new ArrayList<>();
        exists = true;
        positions.add(start);
        for(int i = 1; i < size; i++){
            int r = start.r + (direction == Direction.VERTICAL ? i : 0);
            int c = start.c + (direction == Direction.HORIZONTAL ? i : 0);
            if(r >= 10 || c >= 10)
                exists = false;
            Position next = new Position(r,c);
            positions.add(next);
        }
    }

    public List<Position> getPositions() {
        return positions;
    }

    public int size(){
        return positions.size();
    }
}
