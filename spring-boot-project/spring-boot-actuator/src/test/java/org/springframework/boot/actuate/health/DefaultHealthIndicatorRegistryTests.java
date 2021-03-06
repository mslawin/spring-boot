/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultHealthIndicatorRegistry}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class DefaultHealthIndicatorRegistryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private HealthIndicator one = mock(HealthIndicator.class);

	private HealthIndicator two = mock(HealthIndicator.class);

	private final HealthAggregator healthAggregator = new OrderedHealthAggregator();

	private DefaultHealthIndicatorRegistry registry;

	@Before
	public void setUp() {
		given(this.one.health())
				.willReturn(new Health.Builder().unknown().withDetail("1", "1").build());
		given(this.two.health())
				.willReturn(new Health.Builder().unknown().withDetail("2", "2").build());
		this.registry = new DefaultHealthIndicatorRegistry(this.healthAggregator);
	}

	@Test
	public void register() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		assertThat(this.registry.getAll()).hasSize(2);
		assertThat(this.registry.get("one")).isSameAs(this.one);
		assertThat(this.registry.get("two")).isSameAs(this.two);
	}

	@Test
	public void registerAlreadyUsedName() {
		this.registry.register("one", this.one);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("HealthIndicator with name 'one' already registered");
		this.registry.register("one", this.two);
	}

	@Test
	public void unregister() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		assertThat(this.registry.getAll()).hasSize(2);
		HealthIndicator two = this.registry.unregister("two");
		assertThat(two).isSameAs(this.two);
		assertThat(this.registry.getAll()).hasSize(1);
	}

	@Test
	public void unregisterUnknown() {
		this.registry.register("one", this.one);
		assertThat(this.registry.getAll()).hasSize(1);
		HealthIndicator two = this.registry.unregister("two");
		assertThat(two).isNull();
		assertThat(this.registry.getAll()).hasSize(1);
	}

	@Test
	public void healthWithIndicators() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		Health result = this.registry.health();
		assertThat(result.getDetails()).hasSize(2);
		assertThat(result.getDetails()).containsEntry("one",
				new Health.Builder().unknown().withDetail("1", "1").build());
		assertThat(result.getDetails()).containsEntry("two",
				new Health.Builder().unknown().withDetail("2", "2").build());
	}

	@Test
	public void healthWithNoIndicator() {
		Health result = this.registry.health();
		assertThat(result.getDetails()).isEmpty();
	}

	@Test
	public void healthWithRemovedIndicator() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		HealthIndicator one = this.registry.unregister("one");
		assertThat(one).isEqualTo(this.one);
		Health result = this.registry.health();
		assertThat(result.getDetails()).hasSize(1);
		assertThat(result.getDetails()).containsEntry("two",
				new Health.Builder().unknown().withDetail("2", "2").build());
	}

}
