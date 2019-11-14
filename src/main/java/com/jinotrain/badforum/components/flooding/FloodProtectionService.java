package com.jinotrain.badforum.components.flooding;

import com.jinotrain.badforum.db.entities.ForumUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class FloodProtectionService
{
    private class FloodWindow
    {
        final int      limit;
        final Duration window;

        FloodWindow(int limit, Duration window)
        {
            this.limit    = limit;
            this.window   = window;
        }
    }

    @Autowired
    private Environment environment;

    private Map<FloodCategory, Map<String, FloodTimeTracker>> accessTimes;
    private Map<FloodCategory, FloodWindow> accessLimits;


    public FloodProtectionService()
    {
        accessTimes = new HashMap<>();
        accessLimits = new HashMap<>();

        for (FloodCategory f: FloodCategory.values())
        {
            accessTimes.put(f, new HashMap<>());
        }
    }


    @PostConstruct
    public void postConstruct()
    {
        buildAccessLimits();
    }


    private void buildAccessLimits()
    {
        accessLimits = new HashMap<>();

        for (FloodCategory f: FloodCategory.values())
        {
            String propname = "badforum.flood." + f.name().toLowerCase();

            String limitStr  = environment.getProperty(propname + ".limit", "");
            String windowStr = environment.getProperty(propname + ".window", "");

            int limit;
            Duration window;

            try { limit = Integer.parseInt(limitStr); }
            catch (NumberFormatException e) { limit = f.defaultLimit; }

            try { window = Duration.ofSeconds(Integer.parseInt(windowStr)); }
            catch (NumberFormatException e) { window = f.defaultWindow; }

            FloodWindow fw = new FloodWindow(limit, window);
            accessLimits.put(f, fw);
        }
    }


    public Duration getFloodWindow(FloodCategory category)
    {
        FloodWindow window = accessLimits.get(category);
        return window.window;
    }


    private boolean isFlooding(FloodTimeTracker tracker)
    {
        Instant oldest = tracker.getOldest();
        if (oldest == null) { return false; }

        Instant checkMoment = Instant.now().minus(tracker.getFloodWindow());
        return checkMoment.isBefore(oldest);
    }


    private boolean isIdle(FloodTimeTracker tracker, Instant now)
    {
        Instant newest = tracker.getNewest();
        if (newest == null) { return true; }

        Instant checkMoment = now.minus(tracker.getFloodWindow());
        return !checkMoment.isBefore(newest);
    }


    private String idFrom(Object o) throws UnsupportedOperationException
    {
        if (o instanceof ForumUser)
        {
            return "user:" + ((ForumUser)o).getUsername();
        }

        if (o instanceof Number)
        {
            return "number:" + o.toString();
        }

        if (o instanceof String)
        {
            return "string:" + o;
        }

        throw new UnsupportedOperationException("Cannot track object of type " + o.getClass().getName() + "in" + getClass().getSimpleName());
    }


    public boolean updateIfNotFlooding(FloodCategory category, Object o) throws UnsupportedOperationException
    {
        Map<String, FloodTimeTracker> trackers = accessTimes.get(category);
        FloodTimeTracker tracker;
        String id = idFrom(o);

        if (trackers.containsKey(id))
        {
            tracker = trackers.get(id);
            if (isFlooding(tracker)) { return false; }
        }
        else
        {
            FloodWindow limits = accessLimits.get(category);
            tracker = new FloodTimeTracker(limits.limit, limits.window);
            trackers.put(id, tracker);
        }

        tracker.addCurrentTime();
        return true;
    }


    @Scheduled(initialDelay = (1000 * 60), fixedRate = (1000 * 60))
    public void pruneIdleTrackers()
    {
        Instant now = Instant.now();

        for (Map<String, FloodTimeTracker> trackers: accessTimes.values())
        {
            for (Map.Entry<String, FloodTimeTracker> e: trackers.entrySet())
            {
                String id = e.getKey();
                FloodTimeTracker tracker = e.getValue();
                if (isIdle(tracker, now)) { trackers.remove(id); }
            }
        }
    }
}
