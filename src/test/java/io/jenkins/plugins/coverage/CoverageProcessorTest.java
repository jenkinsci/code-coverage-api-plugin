package io.jenkins.plugins.coverage;

import hudson.FilePath;
import hudson.model.Run;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Acceptance tests for CoverageProcessor. Verifies if set options in CoverageProcessor are used and lead to accepted
 * results.
 */
public class CoverageProcessorTest {

    CoverageProcessor coverageProcessor;
    CoverageResult coverageResult;
    CoverageAction coverageAction;

    @Before()
    public void testInit(){
        coverageProcessor = new CoverageProcessor(mock(Run.class),mock(FilePath.class),null);
        coverageResult =mock(CoverageResult.class);
        coverageAction =mock(CoverageAction.class);
    }

    /**
     * Test roundFloat method in CoverageProcessor
     */
    @Test
    public void testFloatRounding() {
        float number=-0.001f;
        float scaledNumber= CoverageProcessor.roundFloat(number,2);
        Assert.assertEquals(scaledNumber,0.00,0);
    }

    @Test()
    public void testChangeRequestDecreasedCoverageNotFailed_NoChange() throws CoverageException {
        Mockito.when(coverageResult.getCoverageDelta(any())).thenReturn(-0.001f);
        //No Exception should be thrown
        coverageProcessor.failBuildIfChangeRequestDecreasedCoverage(coverageResult,coverageAction);
        verify(coverageAction, times(0)).setFailMessage(any());
    }

    @Test(expected = CoverageException.class)
    public void testChangeRequestDecreasedCoverageFailed_DecCoverage() throws CoverageException {
        Mockito.when(coverageResult.getCoverageDelta(any())).thenReturn(-1.11f);
        coverageProcessor.failBuildIfChangeRequestDecreasedCoverage(coverageResult,coverageAction);
        verify(coverageAction, times(1)).setFailMessage(any());
    }

    @Test()
    public void testChangeRequestDecreasedCoverageNotFailed_IncCoverage() throws CoverageException {
        Mockito.when(coverageResult.getCoverageDelta(any())).thenReturn(0.1f);
        coverageProcessor.failBuildIfChangeRequestDecreasedCoverage(coverageResult,coverageAction);
        verify(coverageAction, times(0)).setFailMessage(any());
    }
}
