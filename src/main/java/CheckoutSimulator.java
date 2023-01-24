import java.util.Queue;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;

/**
 * A class representing simulator that simulates a checkout as a single-server system.
 */
public class CheckoutSimulator {
    // the distribution for inter-arrival times
    protected final AbstractRealDistribution arrivalDistribution;
    // the distribution for service times
    protected final AbstractRealDistribution serviceDistribution;

    // the customer currently being served
    protected Customer currentCustomer = null;
    // the FIFO queue of customers waiting to be served
    protected Queue<Customer> customers;
    // the FEL
    protected PriorityQueue<Event> events;
    // the time at which each simulation should stop
    private final double stopTime;

    // the running total number of served customers
    protected int served;
    // the running total time that all customers have spent in the system
    protected double systemTime;

    /**
     * Simulates the checkout.
     */
    public double[] run() {
        // initialise the system's state
        currentCustomer = null;
        customers = new LinkedList<>();
        events = new PriorityQueue<>();

        // initialise the cumulative statistics
        served = 0;
        systemTime = 0;
        // `busyTime` is the total time that the system has spent serving a customer.
        double busyTime = 0;
        // `customerTotal` is the running area under a graph which displays the number of customers in system over time.
        double customerTotal = 0;

        // initialise the FEL
        // schedule an arrival after a generated inter-arrival time
        double arrivalTime = arrivalDistribution.sample();
        events.add(new ArriveEvent(arrivalTime));
        // schedule the simulation to stop after the provided time
        events.add(new StopEvent(stopTime));

        // the time at which the previous event occurred
        double previousTime = 0;
        while (!events.isEmpty()) {
            // pop an event from the FEL
            Event event = events.remove();

            // the time between the previous and current events
            double delta = event.time - previousTime;
            // the number of customers currently being served
            int servicing = currentCustomer == null ? 0 : 1;
            // update `busyTime`
            busyTime += delta * servicing;
            // update `customerTotal`
            customerTotal += delta * (servicing + customers.size());

            // execute the popped event
            event.execute();
            // update `previousTime`
            previousTime = event.time;
        }

        // calculate the utilisation (the time-average number of customers being serviced), the time-average number of
        // customers in the system, and the customer-average system time
        double utilisation = busyTime / stopTime;
        double meanCustomers = customerTotal / stopTime;
        double meanSystemTime = systemTime / served;
        // return the resulting statistics as a `Result` object
        return new double[] {utilisation, meanCustomers, meanSystemTime};
    }

    /**
     * A constructor that initialises the time at which each simulation should stop and exponentially distributes the
     * inter-arrival and service times.
     * @param stopTime the time at which each simulation should stop
     * @param arrivalRate the mean rate at which customers arrive
     * @param serviceRate the mean rate at which we serve customers
     */
    public CheckoutSimulator(double stopTime, double arrivalRate, double serviceRate) {
        this.arrivalDistribution = new ExponentialDistribution(1 / arrivalRate);
        this.serviceDistribution = new ExponentialDistribution(1 / serviceRate);
        this.stopTime = stopTime;
    }

    /**
     * An abstract class representing an event notice.
     */
    protected static abstract class Event implements Comparable<Event> {
        // the event time
        protected final double time;

        // initialises `time`
        public Event(double time) {
            this.time = time;
        }

        /**
         * Executes this event by altering the system's state and/or putting new events on the FEL.
         */
        public abstract void execute();

        // We compare two events using their times.
        @Override
        public int compareTo(Event other) {
            return Double.compare(time, other.time);
        }
    }

    /**
     * A class representing an arrive event notice.
     * An arrive event enqueues or serves a new customer, based on whether we are currently serving another customer.
     * It schedules the arrival of another customer.
     */
    protected class ArriveEvent extends Event {
        // initialises `time`
        public ArriveEvent(double time) {
            super(time);
        }

        /**
         * Executes this event by enqueuing the new customer iff we are currently serving another customer and,
         * otherwise, serve the customer by scheduling their departure. We schedule the arrival of another customer.
         */
        @Override
        public void execute() {
            // schedule the arrival of another customer after a generated inter-arrival time
            double arrivalTime = time + arrivalDistribution.sample();
            events.add(new ArriveEvent(arrivalTime));

            // `customer` is the customer which arrived.
            Customer customer = new Customer(time);
            if (currentCustomer == null) {
                // If we are not currently serving another customer then we serve `customer`.
                currentCustomer = customer;
                // We schedule `customer`'s departure after a generated service time
                double departTime = time + serviceDistribution.sample();
                events.add(new DepartEvent(departTime));
            } else {
                // Otherwise, enqueue `customer` so that they wait to be served.
                customers.add(customer);
            }
        }
    }

    /**
     * A class representing a depart event notice.
     * A depart event removes the customer that we are currently serving from the checkout. It serves the next customer
     * iff there exists such a customer. It also updates the cumulative statistics for the current simulation.
     */
    protected class DepartEvent extends Event {
        // initialises `time`
        public DepartEvent(double time) {
            super(time);
        }

        /**
         * Executes this event by removing the customer that we are currently serving from the checkout. We then service
         * (schedule the departure of) the next customer iff there exists such a customer. We also update the cumulative
         * statistics for the current simulation.
         */
        @Override
        public void execute() {
            // update the cumulative statistics for the current simulation
            served += 1;
            systemTime += time - currentCustomer.arrivalTime();

            if (customers.isEmpty()) {
                // If there are no waiting customers then the checkout becomes idle.
                currentCustomer = null;
            } else {
                // Otherwise, we service the next customer by scheduling their departure after a generated service time.
                currentCustomer = customers.remove();
                double departTime = time + serviceDistribution.sample();
                events.add(new DepartEvent(departTime));
            }
        }
    }

    /**
     * A class representing a stop event notice.
     * A stop event stops the encompassing simulation.
     */
    protected class StopEvent extends Event {
        // initialises `time`
        public StopEvent(double time) {
            super(time);
        }

        /**
         * Executes this event by clearing the FEL, stopping the simulation.
         */
        @Override
        public void execute() {
            events.clear();
        }
    }
}
