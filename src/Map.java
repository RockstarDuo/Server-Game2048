import java.util.Random;

public class Map {
    private int[][] grid;
    private int width;

    // 맵 객체가 생성될 때 매개변수로 맵 너비를 주고 해당 너비를 이용해 grid와 width를 초기화한다.
    public Map(int width) {
        this.width = width;
        grid = new int[width][width];
        initializeMap();
    }

    private void initializeMap() {
        // 처음에 난수로 '2' 블럭을 생성한다.
        Random rand = new Random();
        // ...
    }
}
