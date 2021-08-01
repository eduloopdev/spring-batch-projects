package com.fronzec.myservice.batch;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;

import com.fronzec.myservice.utils.JsonUtils;
import com.fronzec.myservice.web.LaunchJobRequest;

/**
 * Esta clas se encarga de proveer y gestionar la vida de los jobs
 */
@Service
public class JobsManagerService {

  // TODO esto se debe mover a la BD a un modelo entidad relacion

  /**
   * Saves jobBeanNames and jobsParams
   */
  private Map<String, Map<String, String>> jobs = new HashMap<>();

  private static final Logger logger = LoggerFactory.getLogger(JobsManagerService.class);

  private JobLauncher asyncJobLauncher;

  /**
   * JobExplorer is a readonly implementation of the JobRepository, it can be configured with custom props
   */
  private JobExplorer jobExplorer;

  /**
   * Allow keep track of which jobs are available in the context
   */
  private JobRegistry jobRegistry;

  private BeanFactory beanFactory;

  private List<Job> jobsList;

  /**
   * The JobRepository provides CRUD operations on the meta-data, and the JobExplorer provides
   * read-only operations on the meta-data. However, those operations are most useful when used together to perform common
   * monitoring tasks such as stopping, restarting, or summarizing a Job, as is commonly done by batch operators.
   * Spring Batch provides these types of operations via the JobOperator interface:
   */
  private JobOperator jobOperator;

  public JobsManagerService(JobLauncher asyncJobLauncher, JobExplorer jobExplorer, JobRegistry jobRegistry, BeanFactory beanFactory,
          List<Job> jobsList, JobOperator jobOperator) {
    this.asyncJobLauncher = asyncJobLauncher;
    this.jobExplorer = jobExplorer;
    this.jobRegistry = jobRegistry;
    this.beanFactory = beanFactory;
    this.jobsList = jobsList;
    this.jobOperator = jobOperator;
  }

  @PostConstruct
  public void init() {
    // NOTE: 16/05/21 Jobs configurations associated manually
    Map<String, String> params = new HashMap<>();
    params.put("paramname", "paramvalue");

    jobs.put("myImportUserJob", params);
    jobs.put("importCustomerJob", params);
    // TODO: 16/05/21 trying to run non existing job
    jobs.put("nonExistingJob", params);

    // NOTE: 16/05/21 Jobs loaded in our service using spring, see injection on constructor
    logger.info("jobs found by spring -> {}", jobsList.size());
    jobsList.forEach(job -> logger.info("job-> {}", job));

  }

  public HashMap<String, String> launchAllJobs(Date date, final Integer runningNumber) {
    Objects.requireNonNull(date);
    // TODO: 15/05/21 get all jobs
    // TODO: 16/05/21 instance a job parameter and job names
    HashMap<String, String> info = new HashMap<>(jobs.size());
    jobs.forEach((key, params) -> {
      try {
        var theJob = (Job) beanFactory.getBean(key);
        Objects.requireNonNull(theJob);
        var jobParametersBuilder = new JobParametersBuilder();
        // Adding the date to our param we set and allow run a single job only for one day
        int day = date.getDay();
        int month = date.getMonth();
        int year = date.getYear();
        params.put("DATE", String.format("%s-%s-%s", day, month, year));
        params.put("RUNNING_NUMBER", runningNumber.toString());
        params.forEach(jobParametersBuilder::addString);
        try {
          logger.info("Running job: {} with params -> {}", theJob, params);
          asyncJobLauncher.run(theJob, jobParametersBuilder.toJobParameters());
          info.put(key, theJob.toString());
        } catch (JobExecutionAlreadyRunningException e) {
          logger.error("already", e);
          info.put(key, e.getMessage());
        } catch (JobRestartException e) {
          logger.error("restart", e);
          info.put(key, e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
          logger.error("completed", e);
          info.put(key, e.getMessage());
        } catch (JobParametersInvalidException e) {
          logger.info("parameters", e);
          info.put(key, e.getMessage());
        }
      } catch (NoSuchBeanDefinitionException e) {
        logger.info("nobeanjob found", e);
        info.put(key, e.getMessage());
      }

    });

    return info;
  }

  /**
   * @param date
   * @param runningNumber
   * @return
   */
  public HashMap<String, String> launchAllJobsPreloaded(Date date, final Integer runningNumber) {
    Objects.requireNonNull(date);
    var info = new HashMap<String, String>(jobsList.size());
    jobsList.forEach(job -> {
      var key = job.getName();
      var params = new HashMap<String, String>();
      var jobParametersBuilder = new JobParametersBuilder();
      // Adding the date to our param we set and allow run a single job only for one day
      var day = date.getDay();
      var month = date.getMonth();
      var year = date.getYear();
      params.put("DATE", String.format("%s-%s-%s", day, month, year));
      params.put("RUNNING_NUMBER", runningNumber.toString());
      params.forEach(jobParametersBuilder::addString);
      try {
        logger.info("Running job: {} with params -> {}", job, params);
        asyncJobLauncher.run(job, jobParametersBuilder.toJobParameters());
        info.put(key, job.toString());
      } catch (JobExecutionAlreadyRunningException e) {
        logger.error("already", e);
        info.put(key, e.getMessage());
      } catch (JobRestartException e) {
        logger.error("restart", e);
        info.put(key, e.getMessage());
      } catch (JobInstanceAlreadyCompleteException e) {
        logger.error("completed", e);
        info.put(key, e.getMessage());
      } catch (JobParametersInvalidException e) {
        logger.info("parameters", e);
        info.put(key, e.getMessage());
      }
    });

    return info;
  }

  public Map<String, String> runJobWithParams(LaunchJobRequest request) {
    Map<String, String> launchedJobMetadata = new HashMap<>();
    Map<String, String> jobExecParams = new HashMap<>();
    if (jobs.containsKey(request.getJobBeanName())) {
      try {
        var theJob = (Job) beanFactory.getBean(request.getJobBeanName());
        Objects.requireNonNull(theJob);
        launchedJobMetadata.put("jobName", request.getJobBeanName());
        var jobParametersBuilder = new JobParametersBuilder();
        // Adding the date to our param we track the execution of the job for a day
        // That param allow for example filter data to be processed for that day
        LocalDate execDate = LocalDate.parse(request.getParams()
                .get("date"));
        jobExecParams.put("DATE", String.format("%s-%s-%s", execDate.getDayOfMonth(), execDate.getMonth()
                .getValue(), execDate.getYear()));
        jobExecParams.put("ATTEMPT_NUMBER", request.getParams()
                .get("execution_attempt_number"));
        jobExecParams.put("DESCRIPTION", request.getParams()
                .get("description"));

        // add param to the job builder
        jobExecParams.forEach(jobParametersBuilder::addString);

        logger.info("Attempt to run job -> {} with params -> {}", theJob, JsonUtils.parseObject2Json(jobExecParams));
        asyncJobLauncher.run(theJob, jobParametersBuilder.toJobParameters());
        launchedJobMetadata.put("result", JsonUtils.parseObject2Json(theJob.toString()));

      } catch (JobExecutionAlreadyRunningException e) {
        logger.error("already running job with the same params", e);
        launchedJobMetadata.put("error", e.getMessage());
      } catch (JobRestartException e) {
        logger.error("restart", e);
        launchedJobMetadata.put("error", e.getMessage());
      } catch (JobInstanceAlreadyCompleteException e) {
        logger.error("completed", e);
        launchedJobMetadata.put("error", e.getMessage());
      } catch (JobParametersInvalidException e) {
        logger.info("parameters", e);
        launchedJobMetadata.put("error", e.getMessage());
      }
    } else {
      launchedJobMetadata.put("error", String.format("Job <%s> not found", request.getJobBeanName()));
    }
    return launchedJobMetadata;
  }

  public HashMap<String, String> stopAllJobs() {
    HashMap<String, String> singaledStopped = new HashMap<>();
    jobs.keySet()
            .forEach(name -> {
              try {
                Set<Long> runningExecutions = jobOperator.getRunningExecutions(name);
                logger.info("Stopping job name -> {} :: running -> {} ", name, runningExecutions);
                runningExecutions.forEach(exId -> {
                  try {
                    jobOperator.stop(exId);
                  } catch (NoSuchJobExecutionException e) {
                    logger.error("nosuch", e);
                  } catch (JobExecutionNotRunningException e) {
                    logger.error("no running", e);
                  }
                });
                singaledStopped.put(name, runningExecutions.toString());
              } catch (NoSuchJobException e) {
                singaledStopped.put(name, "NOSUCHJOB");
                logger.error("no such job", e);
              }
            });

    return singaledStopped;
  }

  public HashMap<String, Map<String, String>> getRunning() {
    HashMap<String, Map<String, String>> running = new HashMap<>();
    jobs.keySet()
            .forEach(key -> {
              HashMap<String, String> tmp = new HashMap<>();
              try {
                Set<Long> runningExecutions = jobOperator.getRunningExecutions(key);
                runningExecutions.forEach(r -> {
                  try {
                    tmp.put(String.valueOf(r), jobOperator.getSummary(r));
                  } catch (NoSuchJobExecutionException e) {
                    tmp.put(String.valueOf(r), "NOSUCHEXECUTION");
                  }
                });
                running.put(key, tmp);
              } catch (NoSuchJobException e) {
                logger.error("no such job", e);
                running.put(key, Collections.singletonMap("error", "nosuch job"));
              }
            });
    return running;
  }
}
