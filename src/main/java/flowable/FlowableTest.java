package flowable;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlowableTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(ZonedDateTime.now());

        //time capped rate
/*
            Flowable.range(1, 10)
                .zipWith(Flowable.interval(200, TimeUnit.MILLISECONDS), (item, interval) -> item)
                .subscribe(value -> {
                    //if this takes longer than interval it won't read another request until process finished
                    //if shorter it will wait for interval to read another request
                    Thread.sleep(250);
                    System.out.println(value +":" + ZonedDateTime.now());
                }, System.out::println);*/


        //thread capped rate
        //limits excution to parallel thread count
        Flowable.range(1, 25)
                .parallel(5)
                .runOn(Schedulers.from(Executors.newFixedThreadPool(5)))
                .map(integer -> {
                    Thread.sleep(1000);
                    return integer;
                })
                .sequential()
                .subscribe(value -> System.out.println(value + ":" + ZonedDateTime.now()), System.out::println);


        //

        Thread.sleep(50000);
    }
}
