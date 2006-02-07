/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
#include <netinet/in.h>
#include <sys/socket.h>
#include <netdb.h>

#include <string>
#include <iostream>

#ifdef WITH_UNICODE
#include "unicode.h"
#endif

#include "mary_client.h"


/**
 * Demonstration code for using the mary client.
 + Call this as:
 * ./demo_mary_client (with a text output type)
 * ./demo_mary_client > test.wav (with audio output)
 */
int main()
{
  std::string server_host = "cling.dfki.uni-sb.de";
  int server_port = 59125;
  std::string text = "Welcome to the world of speech synthesis!";

  // To send properly encoded UTF-8 text, you need to convert the text to UTF-8
  // (e.g. by means of the ICU library used by the accompanying unicode.c --
  // get it at http://ibm.com/software/globalization/icu)

#ifdef WITH_UNICODE
  // convert request from ISO-8859-1 to UTF-8
  text = convert_encoding (text, "LATIN-1", "UTF-8");
#endif

  std::string inputType = "TEXT_EN";
  std::string outputType = "ACOUSTPARAMS";
  //std::string outputType = "AUDIO";
  std::string audioType = "WAVE";

  std::string result;
  mary_query(server_host, server_port, text, inputType, outputType, audioType, result);

  if (outputType != "AUDIO") {
#ifdef WITH_UNICODE
    // convert received result from UTF-8 to ISO-8859-1
    result = convert_encoding (result, "UTF-8", "LATIN-1");
#endif
  }

  std::cout << result;
}
