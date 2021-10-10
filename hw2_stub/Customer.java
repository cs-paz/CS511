import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Customer implements Runnable {
    private Bakery bakery;
    private Random rnd;
    private List<BreadType> shoppingCart;
    private int shopTime;
    private int checkoutTime;

    /**
     * Initialize a customer object and randomize its shopping cart
     */
    public Customer(Bakery bakery) {
        this.bakery = bakery;
        this.rnd = new Random();
        this.shoppingCart = new ArrayList<>();
        this.shopTime = rnd.nextInt(250);
        this.checkoutTime = rnd.nextInt(250);
    }

    /**
     * Run tasks for the customer
     */
    public void run() {
            
        try {
            // customer makes shopping list
            this.fillShoppingCart();

            // customer is choosing a shelf
            this.bakery.accessTotalShelves.acquire();

            // customer chooses what bread to buy
            // if shelf is already taken, the customer waits
            for (BreadType b : shoppingCart) {
                Semaphore individualShelfAccess = this.bakery.shelfAccess.get(b);
                individualShelfAccess.acquire();
                Thread.sleep(shopTime / shoppingCart.size()); 
                this.bakery.takeBread(b);
                individualShelfAccess.release();
            }

            // customer leaves the shelves and goes to registers
            this.bakery.accessRegisters.acquire();
            this.bakery.accessTotalShelves.release();

            this.bakery.cashier.acquire();
            // customer is now checking out
            Thread.sleep(checkoutTime); 

            // after customer checks out
            // add transaction to sales
            // cashier is available now
            this.bakery.addSales(this.getItemsValue());
            this.bakery.cashier.release();

            // customer leaves bakery
            this.bakery.accessRegisters.release();

            System.out.println(this);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

    }

    /**
     * Return a string representation of the customer
     */
    public String toString() {
        return "Customer " + hashCode() + ": shoppingCart=" + Arrays.toString(shoppingCart.toArray()) + ", shopTime=" + shopTime + ", checkoutTime=" + checkoutTime;
    }

    /**
     * Add a bread item to the customer's shopping cart
     */
    private boolean addItem(BreadType bread) {
        // do not allow more than 3 items, chooseItems() does not call more than 3 times
        if (shoppingCart.size() >= 3) {
            return false;
        }
        shoppingCart.add(bread);
        return true;
    }

    /**
     * Fill the customer's shopping cart with 1 to 3 random breads
     */
    private void fillShoppingCart() {
        int itemCnt = 1 + rnd.nextInt(3);
        while (itemCnt > 0) {
            addItem(BreadType.values()[rnd.nextInt(BreadType.values().length)]);
            itemCnt--;
        }
    }

    /**
     * Calculate the total value of the items in the customer's shopping cart
     */
    private float getItemsValue() {
        float value = 0;
        for (BreadType bread : shoppingCart) {
            value += bread.getPrice();
        }
        return value;
    }
}