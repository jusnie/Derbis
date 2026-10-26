package com.luc4n3x.derbis.data.network

import com.luc4n3x.derbis.domain.DerbisNetworkTestOutcome
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Test

class DerbisNetworkTesterTest {

    @Test
    fun classifiesDirectAndNestedNetworkFailures() {
        assertEquals(
            DerbisNetworkTestOutcome.DnsResolutionFailed,
            DerbisNetworkTester.classify(UnknownHostException("dns"))
        )
        assertEquals(
            DerbisNetworkTestOutcome.Timeout,
            DerbisNetworkTester.classify(IOException("outer", SocketTimeoutException("timeout")))
        )
        assertEquals(
            DerbisNetworkTestOutcome.Timeout,
            DerbisNetworkTester.classify(InterruptedIOException("interrupted"))
        )
        assertEquals(
            DerbisNetworkTestOutcome.ConnectionRefused,
            DerbisNetworkTester.classify(ConnectException("refused"))
        )
        assertEquals(
            DerbisNetworkTestOutcome.TlsFailure,
            DerbisNetworkTester.classify(SSLHandshakeException("tls"))
        )
        assertEquals(
            DerbisNetworkTestOutcome.ProxyAuthenticationFailed,
            DerbisNetworkTester.classify(IOException("Failed to authenticate with proxy"))
        )
        assertEquals(
            DerbisNetworkTestOutcome.UnknownError,
            DerbisNetworkTester.classify(IllegalStateException("other"))
        )
    }
}
