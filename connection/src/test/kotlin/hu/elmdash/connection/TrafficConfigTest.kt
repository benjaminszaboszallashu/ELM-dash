package hu.elmdash.connection

import org.junit.Assert.*
import org.junit.Test

class TrafficConfigTest {
    @Test fun disabledCanBeSavedWithoutSubscription() { TrafficConfig().validate() }
    @Test fun requiresHttpsSubscriptionWithoutInlineCredentials() {
        listOf("", "http://example.com/feed", "https://user:secret@example.com/feed", "https://example.com/feed#secret", "not a URL").forEach {
            assertTrue(runCatching { TrafficConfig(true, it).validate() }.isFailure)
        }
    }
    @Test fun acceptsTokenInQueryWithoutModifyingIt() {
        TrafficConfig(true,"https://example.com/feed?token=a%2Bb%3D").validate()
    }
}
