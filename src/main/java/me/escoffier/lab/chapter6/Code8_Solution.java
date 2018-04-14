package me.escoffier.lab.chapter6;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.RxHelper;
import me.escoffier.superheroes.SuperHeroesService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static me.escoffier.superheroes.Helpers.log;

public class Code8_Solution {

    private static final int[] SUPER_HEROS_BY_ID = {641, 65, 37, 142};

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        Context context = vertx.getOrCreateContext();
        SuperHeroesService.run(false);

        Scheduler contextScheduler = RxHelper.scheduler(context);

        Observable<String> observable = Observable.<String>create(emitter -> {
            for (int superHeroId : SUPER_HEROS_BY_ID) {
                URL url = new URL("http://localhost:8080/heroes/" + superHeroId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                rd.close();
                String superHero = new JsonObject(result.toString()).getString("name");

                log("Emitting: " + superHero);
                emitter.onNext(superHero);
            }
            log("Completing");
            emitter.onComplete();
        })
            .subscribeOn(Schedulers.io())
            .observeOn(contextScheduler);

        // Execute a Vert.x event loop task
        context.runOnContext(v -> {
            log("---------------- Subscribing");
            observable
                .subscribe(
                    item -> log("Received " + item),
                    error -> log("Error"),
                    () -> log("Complete"));
            log("---------------- Subscribed");
        });
    }
}
