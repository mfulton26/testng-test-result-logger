package io.github.mfulton26.ctrl4testng;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Function;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.ASSERT;

/**
 * @author Mark Fulton
 */
public class CTRL4TestNGTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CTRL4TestNGTest.class);
    private static final CTRL4TestNG CTRL4TESTNG_LOGGER = (CTRL4TestNG) CTRL4TestNG.LOGGER;
    private static final int PARALLEL_TEST_COUNT = 10;

    private final Set<Logger> loggers = Collections.newSetFromMap(new ConcurrentHashMap<Logger, Boolean>());
    private final CountDownLatch countDownLatch = new CountDownLatch(PARALLEL_TEST_COUNT);

    @DataProvider(parallel = true)
    public Iterator<Object[]> idsForUniqueLoggers() {
        return testDataForIdsFrom(Range.closedOpen(0, PARALLEL_TEST_COUNT));
    }

    /**
     * Confirm that each parallel test method gets its own delegated {@link Logger}.
     *
     * @param id an identifier used in the {@link Logger}'s name.
     * @throws InterruptedException if synchronizing ending all the parallel tests together fails.
     */
    @Test(dataProvider = "idsForUniqueLoggers")
    public void getLogger(String id) throws InterruptedException {
        try {
            LOGGER.info("checking for unique logger for current parallel test");
            Logger logger = CTRL4TESTNG_LOGGER.delegate();
            ASSERT.that(loggers.add(logger)).isTrue();
            ASSERT.that(logger.getName()).contains(id);
        }
        finally {
            LOGGER.debug("waiting for other parallel test methods to synchronize return");
            countDownLatch.countDown();
            countDownLatch.await();
        }
    }

    @Test
    public void getSameLogger() {
        ASSERT.that(CTRL4TESTNG_LOGGER.delegate()).isSameAs(CTRL4TESTNG_LOGGER.delegate());
    }

    @Test
    public void loggerCaching() {
        Object cachedLogger = getCachedLogger();
        ASSERT.that(cachedLogger).isNull();
        Logger delegate = CTRL4TESTNG_LOGGER.delegate();
        cachedLogger = getCachedLogger();
        ASSERT.that(delegate).isSameAs(cachedLogger);
        ASSERT.that(cachedLogger.getClass()).isAssignableTo(Logger.class);
        delegate = CTRL4TESTNG_LOGGER.delegate();
        ASSERT.that(delegate).isSameAs(cachedLogger);
        cachedLogger = getCachedLogger();
        ASSERT.that(delegate).isSameAs(cachedLogger);
    }

    private Object getCachedLogger() {
        return Reporter.getCurrentTestResult().getAttribute(CTRL4TestNG.LOGGER_ATTRIBUTE_NAME);
    }

    @Test
    public void badCachedLogger() {
        setCachedLogger("bad");
        ASSERT.that(CTRL4TESTNG_LOGGER.delegate().getClass()).isAssignableTo(Logger.class);
    }

    private void setCachedLogger(Object cachedLogger) {
        Reporter.getCurrentTestResult().setAttribute(CTRL4TestNG.LOGGER_ATTRIBUTE_NAME, cachedLogger);
    }

    private static Iterator<Object[]> testDataForIdsFrom(Range<Integer> range) {
        return FluentIterable.from(ContiguousSet.create(range, DiscreteDomain.integers()))
            .transform(new Function<Integer, Object[]>() {
                @Override
                public Object[] apply(Integer input) {
                    return new Object[] { input.toString() };
                }
            })
            .iterator();
    }

}
