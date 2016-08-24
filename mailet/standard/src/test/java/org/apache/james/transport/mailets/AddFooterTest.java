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

package org.apache.james.transport.mailets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MailUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(Parameterized.class)
public class AddFooterTest {

    @Rule public ExpectedException exception = ExpectedException.none();
    
    private final String javaCharset;
    private final String javaMailCharset;
    private Mailet mailet;
    private FakeMailetConfig mailetConfig;
    
    @Parameters
    public static Collection<String[]> data() {
        //javamail has its own charset handling logic, it needs to be exercised
        List<String[]> charsets = Lists.newArrayList();
        Set<String> charsetNamesToTest = Sets.newHashSet(
                "ANSI_X3.4-1968", 
                "iso-ir-6", 
                "ANSI_X3.4-1986", 
                "ISO_646.irv:1991", 
                "ASCII", 
                "ISO646-US", 
                "US-ASCII",
                "us", 
                "IBM367", 
                "cp367",
                "csASCII");
        Set<List<String>> cartesianProduct = Sets.cartesianProduct(ImmutableList.of(charsetNamesToTest, charsetNamesToTest));
        for (List<String> pair: cartesianProduct) {
            charsets.add(new String[]{pair.get(0), pair.get(1)});
        }
        return charsets;
    }
    
    public AddFooterTest(String javaCharset, String javaMailCharset) {
        this.javaCharset = javaCharset;
        this.javaMailCharset = javaMailCharset;
    }
    
    @Before
    public void setup() {
        mailet = new AddFooter();
        mailetConfig = new FakeMailetConfig("Test", new FakeMailContext());

    }
    
    @Test
    public void shouldAddFooterWhenQuotedPrintableTextPlainMessage() throws MessagingException, IOException {

        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String quotedPrintableTextPlainMessage = Joiner.on("\r\n").join(
                "Subject: test",
                "Content-Type: text/plain; charset=ISO-8859-15",
                "MIME-Version: 1.0",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "Test=E0 and one =A4",
                "");

        String expectedFooter = "------ my footer =E0/=A4 ------";

        Mail mail = FakeMail.fromMime(quotedPrintableTextPlainMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(quotedPrintableTextPlainMessage + expectedFooter);

    }

    @Test
    public void shouldEnsureCarriageReturnWhenAddFooterWithTextPlainMessage() throws MessagingException, IOException {

        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String quotedPrintableTextPlainMessage = Joiner.on("\r\n").join(
                "Subject: test",
                "Content-Type: text/plain; charset=ISO-8859-15",
                "MIME-Version: 1.0",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "Test=E0 and one =A4");

        String expected = Joiner.on("\r\n").join(
                "Subject: test",
                "Content-Type: text/plain; charset=ISO-8859-15",
                "MIME-Version: 1.0",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "Test=E0 and one =A4",
                "------ my footer =E0/=A4 ------");
        

        Mail mail = FakeMail.fromMime(quotedPrintableTextPlainMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(expected);

    }
    
    @Test
    public void shouldNotAddFooterWhenUnsupportedEncoding() throws MessagingException, IOException {
        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String quotedPrintableTextPlainMessage = Joiner.on("\r\n").join(
                "Subject: test",
                "Content-Type: text/plain; charset=UNSUPPORTED_ENCODING",
                "MIME-Version: 1.0",
                "Content-Transfer-Encoding: quoted-printable",
                "", 
                "Test=E0 and one",
                "");

        Mail mail = FakeMail.fromMime(quotedPrintableTextPlainMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(quotedPrintableTextPlainMessage);
    }

    @Test
    public void shouldNotAddFooterWhenUnsupportedTextContentType() throws MessagingException, IOException {
        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String quotedPrintableTextPlainMessage = Joiner.on("\r\n").join(
                "Subject: test",
                "Content-Type: text/calendar; charset=ASCII",
                "MIME-Version: 1.0",
                "Content-Transfer-Encoding: quoted-printable",
                "", 
                "Test=E0 and one",
                "");

        Mail mail = FakeMail.fromMime(quotedPrintableTextPlainMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(quotedPrintableTextPlainMessage);
    }
    
    /*
     * Test for JAMES-443
     * This should not add the header and should leave the multipart/mixed Content-Type intact
     */
    @Test
    public void shouldNotAddFooterWhenNestedUnsupportedMultipart() throws MessagingException, IOException {
        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String quotedPrintableMultipartMixedMessage = Joiner.on("\r\n").join(
                "MIME-Version: 1.0",
                "Content-Type: multipart/mixed; boundary=\"===============0204599088==\"",
                "",
                "This is a cryptographically signed message in MIME format.",
                "",
                "--===============0204599088==",
                "Content-Type: multipart/unsupported; boundary=\"------------ms050404020900070803030808\"",
                "",
                "--------------ms050404020900070803030808",
                "Content-Type: text/plain; charset=ISO-8859-1",
                "",
                "test",
                "",
                "--------------ms050404020900070803030808--",
                "",
                "--===============0204599088==--",
                "");

        Mail mail = FakeMail.fromMime(quotedPrintableMultipartMixedMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(quotedPrintableMultipartMixedMessage);
    }
    
    /*
     * Test for JAMES-368
     */
    @Test
    public void shouldAddFooterWhenMultipartRelatedHtmlMessage() throws MessagingException, IOException {

        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String htmlMultipartRelatedMessagePart1 = Joiner.on("\r\n").join(
                "MIME-Version: 1.0",
                "Subject: test",
                "Content-Type: multipart/related;",
                "  boundary=\"------------050206010102010306090507\"",
                "",
                "--------------050206010102010306090507",
                "Content-Type: text/html; charset=ISO-8859-15",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">",
                "<html>",
                "<head>",
                "<meta content=3D\"text/html;charset=3DISO-8859-15\" http-equiv=3D\"Content-Typ=",
                "e\">",
                "</head>",
                "<body bgcolor=3D\"#ffffff\" text=3D\"#000000\">",
                "<br>",
                "<div class=3D\"moz-signature\">-- <br>",
                "<img src=3D\"cid:part1.02060605.123@zzz.com\" border=3D\"0\"></div>",
                "");
        
        String htmlMultipartRelatedMessagePart2 = Joiner.on("\r\n").join(
                "</body>",
                "</html>",
                "",
                "--------------050206010102010306090507",
                "Content-Type: image/gif",
                "Content-Transfer-Encoding: base64",
                "Content-ID: <part1.02060605.123@zzz.com>",
                "Content-Disposition: inline;",
                "",
                "YQ==",
                "--------------050206010102010306090507--",
                "");

        String expectedFooter = "<br />------ my footer =E0/=A4 ------";

        Mail mail = FakeMail.fromMime(htmlMultipartRelatedMessagePart1 + htmlMultipartRelatedMessagePart2, javaCharset, javaMailCharset);
        mailet.service(mail);

        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(htmlMultipartRelatedMessagePart1 + expectedFooter + htmlMultipartRelatedMessagePart2);
    }
    
    @Test
    public void shouldAddFooterWhenMultipartAlternivateMessage() throws MessagingException,
            IOException {

        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String multipartAlternativeMessage = Joiner.on("\r\n").join(
                "Subject: test",
                "Content-Type: multipart/alternative;",
                "    boundary=\"--==--\"",
                "MIME-Version: 1.0",
                "",
                "----==--",
                "Content-Type: text/plain;",
                "    charset=\"ISO-8859-15\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "Test=E0 and @=80",
                "",
                "----==--",
                "Content-Type: text/html;",
                "    charset=\"CP1252\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "<html><body>test =80 ss</body></html>",
                "----==----"
                );
        
        Mail mail = FakeMail.fromMime(multipartAlternativeMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        String expected = Joiner.on("\r\n").join(
                "Subject: test",
                "Content-Type: multipart/alternative;",
                "    boundary=\"--==--\"",
                "MIME-Version: 1.0",
                "",
                "----==--",
                "Content-Type: text/plain;",
                "    charset=\"ISO-8859-15\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "Test=E0 and @=80",
                "------ my footer =E0/=A4 ------",
                "----==--",
                "Content-Type: text/html;",
                "    charset=\"CP1252\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "<html><body>test =80 ss<br />------ my footer =E0/=80 ------</body></html>",
                "----==----",
                ""
                );
        
        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(expected);
    }

    @Test
    public void shouldAddFooterWhenHtmlMessageWithMixedCaseBodyTag() throws MessagingException,
            IOException {

        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String htmlMessage = Joiner.on("\r\n").join(
                "Subject: test",
                "MIME-Version: 1.0",
                "Content-Type: text/html;",
                "    charset=\"CP1252\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "<html><body>test =80 ss</bOdY></html>",
                ""
                );
        
        Mail mail = FakeMail.fromMime(htmlMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        String expected = Joiner.on("\r\n").join(
                "Subject: test",
                "MIME-Version: 1.0",
                "Content-Type: text/html;",
                "    charset=\"CP1252\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "<html><body>test =80 ss<br />------ my footer =E0/=80 ------</bOdY></html>",
                ""
                );
        
        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(expected);
    }

    @Test
    public void shouldAddFooterWhenHtmlMessageWithNoBodyTag() throws MessagingException,
            IOException {

        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        String htmlMessage = Joiner.on("\r\n").join(
                "Subject: test",
                "MIME-Version: 1.0",
                "Content-Type: text/html;",
                "    charset=\"CP1252\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "<html><body>test =80 ss",
                ""
                );
        
        Mail mail = FakeMail.fromMime(htmlMessage, javaCharset, javaMailCharset);
        mailet.service(mail);

        String expected = Joiner.on("\r\n").join(
                "Subject: test",
                "MIME-Version: 1.0",
                "Content-Type: text/html;",
                "    charset=\"CP1252\"",
                "Content-Transfer-Encoding: quoted-printable",
                "",
                "<html><body>test =80 ss",
                "<br />------ my footer =E0/=80 ------"
                );
        
        assertThat(MailUtil.toString(mail, javaCharset)).isEqualTo(expected);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void shouldThrowMessagingExceptionWhenIOExceptionReadingMessage() throws MessagingException {
        mailetConfig.setProperty("text", "------ my footer \u00E0/\u20AC ------");
        mailet.init(mailetConfig);
        
        Mail mail = mock(Mail.class);
        when(mail.getMessage()).thenThrow(IOException.class);
        exception.expect(MessagingException.class);
        
        mailet.service(mail);
    }
}
