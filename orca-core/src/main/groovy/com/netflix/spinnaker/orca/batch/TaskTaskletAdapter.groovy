/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.batch

import groovy.transform.CompileStatic
import com.netflix.spinnaker.orca.RetryableTask
import com.netflix.spinnaker.orca.Task
import com.netflix.spinnaker.orca.batch.adapters.RetryableTaskTasklet
import com.netflix.spinnaker.orca.batch.adapters.TaskTasklet
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.target.SingletonTargetSource
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.backoff.Sleeper
import org.springframework.retry.backoff.ThreadWaitSleeper
import static org.springframework.retry.interceptor.RetryInterceptorBuilder.stateless

@CompileStatic
class TaskTaskletAdapter {

  private final Sleeper sleeper

  TaskTaskletAdapter(Sleeper sleeper) {
    this.sleeper = sleeper
  }

  TaskTaskletAdapter() {
    this(new ThreadWaitSleeper())
  }

  Tasklet decorate(Task task) {
    if (task instanceof RetryableTask) {
      def tasklet = new RetryableTaskTasklet(task)
      def proxyFactory = new ProxyFactory(Tasklet, new SingletonTargetSource(tasklet))
      def backOffPolicy = new FixedBackOffPolicy(backOffPeriod: task.backoffPeriod)
      if (sleeper) {
        backOffPolicy.sleeper = sleeper
      }
      proxyFactory.addAdvice(stateless().backOffPolicy(backOffPolicy).build())
      return proxyFactory.proxy as Tasklet
    } else {
      return new TaskTasklet(task)
    }
  }

}
