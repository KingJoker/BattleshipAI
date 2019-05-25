import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Battleship {
    public static final byte EMPTY = '_';
    public static final byte POTENTIAL_MOVE = 'o';
    public static final byte POTENTIAL_HIT = 'x';
    public static final byte MOVE = 'O';
    public static final byte HIT = 'X';
    public static final byte SUNK_SHIP = '#';
    private List<Ship> ships;
    private List<Ship> sunkShips;
    public byte[][] board;

    public static void main(String[] args){
        Battleship battleship = new Battleship();
        byte[][] board = battleship.getBoard();
        int moves = 0;
        while(battleship.ships.size() > 0){
            battleship.makeMove(battleship.findNextMove());
            System.out.println(moves++);
            System.out.println(battleship);
            try {
                //Thread.sleep(500);
            }
            catch (Exception e){}
        }

    }

    public Battleship(){
        board = new byte[10][10];
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                board[r][c] = EMPTY;
            }
        }
        ships = new ArrayList<>();
        sunkShips = new ArrayList<>();
        createShips();
    }

    private void createShips() {
        int[] shipSizes = new int[]{2,3,3,4,5};
        Set<Position> previousShipPositions = new HashSet<>();
        Random rand = new Random();
        for (int i = 0; i < shipSizes.length; i++) {
            boolean placeSuccessful = false;
            Ship ship = null;
            CreateShip:while(!placeSuccessful){
                int r = rand.nextInt(10);
                int c = rand.nextInt(10);
                ship = new Ship(new Position(r,c),shipSizes[i],rand.nextBoolean()? Ship.Direction.HORIZONTAL: Ship.Direction.VERTICAL);
                if(!ship.exists)
                    continue CreateShip;
                List<Position> shipPositions = ship.getPositions();
                for (Position position : shipPositions) {
                    if (previousShipPositions.contains(position))
                        continue CreateShip;
                }
                placeSuccessful = true;
            }
            ships.add(ship);
            previousShipPositions.addAll(ship.getPositions());
        }
    }

    public byte[][] getBoard() {
        return board;
    }

    public void makeMove(Position position){
        boolean shipPosition = false;
        for(Ship ship : ships){
            List<Position> shipPositions = ship.getPositions();
            if(shipPositions.contains(position)){
                shipPosition = true;
                board[position.r][position.c] = HIT;
                boolean sunk = true;
                for(Position pos : shipPositions){
                    if(board[pos.r][pos.c] != HIT ) {
                        sunk = false;
                        break;
                    }
                }
                if(sunk){
                    for(Position pos : shipPositions){
                        board[pos.r][pos.c] = SUNK_SHIP;
                    }
                    sunkShips.add(ship);
                }
            }
        }
        ships.removeAll(sunkShips);
        if(!shipPosition) {
            board[position.r][position.c] = MOVE;
        }
    }

    public Position findNextMove(){
        Random rand = new Random();
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                if(board[r][c] == HIT){
                    int countVertical = 0;
                    for(int r1 = r+1; r1 < board.length && board[r1][c] == HIT; r1++)
                        countVertical++;
                    for(int r1 = r-1; r1 >= 0 && board[r1][c] == HIT; r1--)
                        countVertical++;
                    int countHorizontal = 0;
                    for(int c1 = c+1; c1 < board[r].length && board[r][c1] == HIT; c1++)
                        countHorizontal++;
                    for(int c1 = c-1; c1 >=0 && board[r][c1] == HIT; c1--)
                        countHorizontal++;
                    Position current = new Position(r,c);
                    int[][] verticals = new int[][]{{1,0},{-1,0}};
                    int[][] horizontals = new int[][]{{0,1},{0,-1}};
                    List<int[]> verticalList = new ArrayList<>();
                    verticalList.add(verticals[0]);
                    verticalList.add(verticals[1]);
                    Collections.shuffle(verticalList,rand);
                    List<int[]> horizontalList = new ArrayList<>();
                    horizontalList.add(horizontals[0]);
                    horizontalList.add(horizontals[1]);
                    Collections.shuffle(horizontalList,rand);
                    List<int[]> directionList = new ArrayList<>();
                    if(countVertical > countHorizontal || ((countHorizontal == countVertical) && rand.nextBoolean())){
                        directionList.addAll(verticalList);
                        directionList.addAll(horizontalList);
                    }
                    else{
                        directionList.addAll(horizontalList);
                        directionList.addAll(verticalList);
                    }
                    for(int i = 0; i < directionList.size(); i++){
                        int[] diffVector = directionList.get(i);
                        Position nextEmpty = findNextEmptyForHit(current,diffVector[0],diffVector[1]);
                        if(nextEmpty != null){
                            return nextEmpty;
                        }
                    }
                }
            }
        }
        return findBestMove();
    }

    public Position findNextEmptyForHit(Position startPosition, int rDiff, int cDiff){
        int count = 1;
        Position nextPosition = new Position(startPosition.r + rDiff, startPosition.c+cDiff);
        while(nextPosition.r >= 0 && nextPosition.c >= 0 && nextPosition.r < board.length && nextPosition.c < board.length){
            if(board[nextPosition.r][nextPosition.c] == MOVE)
                return null;
            if(board[nextPosition.r][nextPosition.c] == EMPTY)
                return nextPosition;
            count++;
            nextPosition = new Position(startPosition.r + rDiff*count, startPosition.c+cDiff*count);
        }
        return null;
    }

    public Position findBestMove(){
        Map<Integer,List<byte[][]>> shipMapsBySize = new HashMap<>();
        Set<Integer> shipSizes = new HashSet<>();
        for(Ship ship : ships){
            shipSizes.add(ship.size());
        }
        for(int size : shipSizes){
            List<byte[][]> shipMap = calculateShipMaps(size);
            shipMapsBySize.put(size,shipMap);
        }
        List<byte[][]> shipMaps = new ArrayList<>();
        for(Ship ship : ships){
            shipMaps.addAll(shipMapsBySize.get(ship.size()));
        }
        Map<Position,Integer> heatMap = createHeatMap(shipMaps);
        return findBestMoveFromHeatMap(heatMap);
    }

    public List<byte[][]> calculateShipMaps(int shipSize){
        List<byte[][]> newMaps = Collections.synchronizedList(new ArrayList<>());
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                /// Vertical placement of ship
                byte[][] newBoardVertical = cloneBoard(board);
                boolean canPlaceShip = true;
                for (int i = r; i < r + shipSize; i++) {
                    if (i >= 10) {
                        canPlaceShip = false;
                        break;
                    }
                    byte value = newBoardVertical[i][c];
                    if (value == HIT || value == MOVE || value == POTENTIAL_HIT || value == SUNK_SHIP) {
                        canPlaceShip = false;
                        break;
                    }
                }
                if (canPlaceShip) {
                    for (int i = r; i < r + shipSize; i++) {
                        newBoardVertical[i][c] = POTENTIAL_HIT;
                    }
                    newMaps.add(newBoardVertical);
                }
                /// End Vertical placement of ship

                /// Horizontal placement of ship
                byte[][] newBoardHorizontal = cloneBoard(board);
                canPlaceShip = true;
                for (int i = c; i < c + shipSize; i++) {
                    if (i >= 10) {
                        canPlaceShip = false;
                        break;
                    }
                    byte value = newBoardHorizontal[r][i];
                    if (value == HIT || value == MOVE || value == POTENTIAL_HIT || value == SUNK_SHIP) {
                        canPlaceShip = false;
                        break;
                    }
                }
                if (canPlaceShip) {
                    for (int i = c; i < c + shipSize; i++) {
                        newBoardHorizontal[r][i] = POTENTIAL_HIT;
                    }
                    newMaps.add(newBoardHorizontal);
                }
                /// End Horizontal placement of ship
            }
        }
        return newMaps;
    }

    public static Map<Position,Integer> createHeatMap(List<byte[][]> maps){
        Map<Position,Integer> heatMap = new ConcurrentHashMap<>();
        maps.parallelStream().forEach((map) ->{
            for(int r = 0; r < 10; r++){
                for(int c = 0; c < 10; c++){
                    if(map[r][c] == POTENTIAL_HIT){
                        heatMap.merge(new Position(r,c),1,(oldVal,one)->oldVal + one);
                    }
                }
            }
        });
        return heatMap;
    }

    public static Position findBestMoveFromHeatMap(Map<Position,Integer> heatMap){
        List<Position> bestPositions = new ArrayList<>();
        int max = Integer.MIN_VALUE;
        for(Position pos : heatMap.keySet()){
            int heat = heatMap.get(pos);
            if(heat == max){
                bestPositions.add(pos);
            }
            if(heat > max){
                bestPositions = new ArrayList<>();
                bestPositions.add(pos);
                max = heat;
            }
        }
        return bestPositions.get((new Random()).nextInt(bestPositions.size()));
    }

    public static List<byte[][]> blockShip(byte[][] board, List<Position> ship){
        List<byte[][]> ret = new ArrayList<>();
        boolean needsFill = true;
        for(Position pos : ship){
            if(board[pos.r][pos.c] != EMPTY){
                needsFill = false;
                break;
            }
        }
        if(!needsFill){
            ret.add(board);
            return ret;
        }
        for(Position pos : ship){
            byte[][] tempBoard = cloneBoard(board);
            tempBoard[pos.r][pos.c] = POTENTIAL_MOVE;
            ret.add(tempBoard);
        }
        return ret;
    }

    public static byte[][] cloneBoard(byte[][] board){
        byte[][] newBoard = new byte[board.length][board[0].length];
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                newBoard[r][c]=board[r][c];
            }
        }
        return newBoard;
    }

    public static String printBoards(List<byte[][]> boards){
        String ret = "";
        for(byte[][] board : boards){
            ret += Arrays.deepToString(board).replace("],","]\n") + "\n";
        }
        return ret;
    }

    public static void printBoard(byte[][] board){
        System.out.println(Arrays.deepToString(board).replace("],","]\n"));
    }

    @Override
    public String toString() {
        String output = "";
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                output += ((char)board[r][c]) + " ";
            }
            output += "\n";
        }
        return output.trim();
    }

}

/*public static List<byte[][]> findMoves(byte[][] board, int shipSize){
        List<byte[][]> currentBoards = new ArrayList<>();
        currentBoards.add(board);
        int count = 1;
        for(int r = 0; r < board.length - shipSize; r++){
            for(int c = 0; c < board[r].length; c++){
                System.out.println((count++) +" " + currentBoards.size());
               // System.out.println(printBoards(currentBoards));
                List<Position> ship = new ArrayList<>();
                for(int r1 = r; r1 < r+shipSize; r1++){
                    ship.add(new Position(r1,c));
                }
                List<byte[][]> newBoards = new ArrayList<>();
                for(byte[][] currentBoard : currentBoards){
                    newBoards.addAll(blockShip(currentBoard,ship));
                }
                currentBoards = newBoards;
            }
        }
        for(int r = 0; r < board.length; r++){
            for(int c = 0; c < board[r].length - shipSize; c++){
                List<Position> ship = new ArrayList<>();
                for(int c1 = c; c1 < c+shipSize; c1++){
                    ship.add(new Position(r,c1));
                }
                List<byte[][]> newBoards = new ArrayList<>();
                for(byte[][] currentBoard : currentBoards){
                    newBoards.addAll(blockShip(currentBoard,ship));
                }
                currentBoards = newBoards;
            }
        }
        return currentBoards;
    }*/

/*public static List<byte[][]> findAllMoves(byte[][] board, int r, int c){

        List<byte[][]> ret = Collections.synchronizedList(new ArrayList<>());
        int newC = c + 1;
        int newR = r;
        if(newC == board[r].length){
            newR++;
            newC = 0;
        }
        if(newR == board.length){
            ret.add(board);
            return ret;
        }
        if(board[r][c] == EMPTY){
            byte[][] boardWithMove = cloneBoard(board);
            byte[][] boardWithoutMove = cloneBoard(board);
            boardWithMove[r][c] = POTENTIAL_MOVE;
            final int futureR = newR;
            final int futureC = newC;
            //ret.addAll(findAllMoves(boardWithMove,futureR,futureC));
            //ret.addAll(findAllMoves(boardWithoutMove,futureR,futureC));
            CompletableFuture<List<byte[][]>> withMove = CompletableFuture.supplyAsync(
                    () -> findAllMoves(boardWithMove,futureR,futureC), Executors.newFixedThreadPool(10));
            CompletableFuture<List<byte[][]>> withoutMove = CompletableFuture.supplyAsync(
                    () -> findAllMoves(boardWithoutMove,futureR,futureC), Executors.newFixedThreadPool(10));

            ret.addAll(withMove.join());
            ret.addAll(withoutMove.join());

        }
        else{
            byte[][] newBoard = cloneBoard(board);
            ret.addAll(findAllMoves(newBoard,newR,newC));
        }
        System.out.println("r:"+r+" c:"+c);
        printBoard(board);
        return ret;
    }*/
