import java.util.Random;

public class Dice {
    private int[] dices;

    public Dice() {
        //constructor of dices
        dices = new int[2];

    }

    public int[] getDices()
    {
        return this.dices;
    }

    public void setDices(int dice1, int dice2) {
        this.dices[0] = dice1;
        this.dices[1] = dice2;
    }
    public void throwTwoDices() {
        //throws the dices and returns an array with them
        Random rand = new Random();
        this.dices[0] = 1 + rand.nextInt(6);
        this.dices[1] = 1 + rand.nextInt(6);
    }
}