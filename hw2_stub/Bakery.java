import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class Bakery implements Runnable {
    private static final int TOTAL_CUSTOMERS = 200;
    private static final int ALLOWED_CUSTOMERS = 50;
    private static final int FULL_BREAD = 20;
    private Map<BreadType, Integer> availableBread;
    private ExecutorService executor;
    private float sales = 0;

    // TODO

    public Semaphore accessTotalShelves = new Semaphore(3);
    public Semaphore accessRegisters = new Semaphore(4);
    public Semaphore cashier = new Semaphore(1);
    public Map<BreadType, Semaphore> shelfAccess;

    // Semaphores for shelfAccess
    private Semaphore ryeAccess = new Semaphore(1);
    private Semaphore sourdoughAccess = new Semaphore(1);
    private Semaphore wonderAccess = new Semaphore(1);

    /**
     * Remove a loaf from the available breads and restock if necessary
     */
    public void takeBread(BreadType bread) {
        int breadLeft = availableBread.get(bread);
        if (breadLeft > 0) {
            availableBread.put(bread, breadLeft - 1);
        } else {
            System.out.println("No " + bread.toString() + " bread left! Restocking...");
            // restock by preventing access to the bread stand for some time
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            availableBread.put(bread, FULL_BREAD - 1);
        }
    }

    /**
     * Add to the total sales
     */
    public void addSales(float value) {
        sales += value;
    }

    /**
     * Run all customers in a fixed thread pool
     */
    public void run() {
        availableBread = new ConcurrentHashMap<BreadType, Integer>();
        availableBread.put(BreadType.RYE, FULL_BREAD);
        availableBread.put(BreadType.SOURDOUGH, FULL_BREAD);
        availableBread.put(BreadType.WONDER, FULL_BREAD);

        // TODO
        shelfAccess = new ConcurrentHashMap<BreadType, Semaphore>();
        shelfAccess.put(BreadType.RYE, ryeAccess);
        shelfAccess.put(BreadType.SOURDOUGH, sourdoughAccess);
        shelfAccess.put(BreadType.WONDER, wonderAccess);
        
        executor = Executors.newFixedThreadPool(ALLOWED_CUSTOMERS);

        for (int i = 0; i < TOTAL_CUSTOMERS; i++) {
            executor.execute(new Customer(this));
        }
        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            DecimalFormat df = new DecimalFormat("0.##"); 
            System.out.println("Total Sales: $" + df.format(sales));
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }
        
    }
}