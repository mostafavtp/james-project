/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class EventIdTest {

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(EventStore.History.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    public void firstShouldReturnAConstant() {
        assertThat(EventId.first())
            .isEqualTo(EventId.first());
    }

    @Test
    public void previousShouldReturnEmptyWhenBeforeFirst() {
        assertThat(EventId.first().previous())
            .isEmpty();
    }

    @Test
    public void compareToShouldReturnNegativeWhenComparedToNext() {
        assertThat(EventId.first())
            .isLessThan(EventId.first().next());
    }

    @Test
    public void compareToShouldReturnNegativeWhenComparedToPrevious() {
        assertThat(EventId.first().next())
            .isGreaterThan(EventId.first());
    }

    @Test
    public void nextShouldAlwaysHaveTheSameIncrement() {
        assertThat(EventId.first().next())
            .isEqualTo(EventId.first().next());
    }

    @Test
    public void previousShouldRevertNext() {
        assertThat(EventId.first().next().previous())
            .contains(EventId.first());
    }

    @Test
    public void compareToShouldReturnNegativeWhenComparedToNextWithPreviousCall() {
        assertThat(EventId.first().next().previous().get())
            .isLessThan(EventId.first().next());
    }

    @Test
    public void compareToShouldReturnNegativeWhenComparedToPreviousWithPreviousCall() {
        assertThat(EventId.first().next())
            .isGreaterThan(EventId.first().next().previous().get());
    }

}