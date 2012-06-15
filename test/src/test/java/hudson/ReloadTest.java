package hudson;


import java.io.IOException;

import hudson.model.FreeStyleBuild;
import hudson.model.Queue.Executable;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.junit.JUnitResultArchiver;

import org.apache.tools.ant.Executor;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.jvnet.hudson.reactor.ReactorException;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TouchBuilder;
import org.jvnet.hudson.test.SleepBuilder;

/*
 * Tests around reload functionality to address https://issues.jenkins-ci.org/browse/JENKINS-3265.
 * @author mmitchell
 */
public class ReloadTest extends HudsonTestCase {

    FreeStyleProject project;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        project = createFreeStyleProject("reload me");
        project.getBuildersList().add(new SleepBuilder(1000 * 5));
    }

    
    @Test
    public void testReloadWhileJobRunning() throws ReactorException, InterruptedException, IOException {
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0);
        Thread.sleep(1000);
        assertEquals(project.getBuilds().size(), 1);
        
        FreeStyleBuild build = project.getBuilds().getFirstBuild();
        assertEquals(build.getNumber(), 1);
        assertEquals(true, build.isBuilding());
        assertEquals(true, build.isLogUpdated());

        for (Computer c : jenkins.getComputers()) {
            for (hudson.model.Executor e : c.getExecutors()) {
                if (e.isBusy() && (e.getCurrentExecutable() instanceof AbstractBuild)) {
                    AbstractBuild runningBuild = (AbstractBuild) e.getCurrentExecutable();
                    runningBuild.save();
                }
            }
        }
        jenkins.reload();

        FreeStyleProject loadedProject = (FreeStyleProject) jenkins.getProjects().get(0);        
        FreeStyleBuild loadedBuild = loadedProject.getBuilds().getFirstBuild();
        assertNotNull(loadedBuild);
        assertEquals(1, loadedBuild.getNumber());
        assertEquals(true, loadedBuild.isBuilding());
        assertEquals(true, loadedBuild.isLogUpdated());        
    }
    
}
