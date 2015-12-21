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
#include <netdb.h>
#include <stdlib.h>
#include <iostream>
#include <string.h>
#include <unistd.h>

#include "MaryClient.h"

using namespace std;

/**
 * A C++ implementation of a simple client to the MARY TTS system.
 * result: an empty string serving as the container for the output.
 *    It will return text or audio data; text data will be encoded as UTF-8.
 * inputText: the UTF-8 encoded text (or XML document) to send as a request
 * maryInFormat: the input type of the data in inputText, e.g. TEXT
 * maryOutFormat: the output type to produce, e.g. MBROLA, AUDIO
 * locale: the language of the input, e.g. EN-US, DE
 * audioType: for AUDIO output, the type of audio data to produce,
 *            e.g. WAVE or MP3.
 * voice: the voice to be used, e.g. cmu-slt-hsmm, bits3.
 * effects: the list of effects to be generated.
 * return value: 0 on success, negative on failure.
 */
int
MaryClient::maryQuery( int server_port,
                       string server_host,
                       string& result,
                       string inputText,
                       string maryInFormat,
                       string maryOutFormat,
                       string locale,
                       string audioType,
                       string voice,
                       string effects ) {

  // prepare the request
  string query = "MARY";
  query += " IN=" + maryInFormat;
  query += " OUT=" + maryOutFormat;
  query += " LOCALE=" + locale;	// remove this line, if using an older version than MARY 4.0
  query += " AUDIO=" + audioType;
  query += " VOICE=" + voice;
  if (effects != "") {
    query += " EFFECTS=" + effects;
  }
  query += "\012\015";

  //cout << "Constructed query: " << query << endl;

  // declare connection stuff
  struct sockaddr_in maryServer;
  struct sockaddr_in maryClient;
  struct hostent* hostInfo;

  // declare variables
  int maryInfoSocket;
  int maryDataSocket;

  // set configuration parameters

  // get host information
  hostInfo = gethostbyname (server_host.c_str());

  if (hostInfo == NULL)
  {
    return -2;
  }


  // create a tcp connection to the mary server
  maryInfoSocket = socket (AF_INET, SOCK_STREAM, 0);

  // verify that the socket could be opened successfully
  if (maryInfoSocket == -1)
  {
    return -2;
  }
  else
  // autoflush stdout, bind and connect
  {
    maryClient.sin_family = AF_INET;
    maryClient.sin_port = htons (0);
    maryClient.sin_addr.s_addr = INADDR_ANY;

    int status = bind (maryInfoSocket, (struct sockaddr*) &maryClient, sizeof (maryClient));

    if (status != 0)
    {
      return -2;
    }

    maryServer.sin_family = AF_INET;
    maryServer.sin_port = htons (server_port);
    memcpy ((char*) &maryServer.sin_addr.s_addr, hostInfo->h_addr_list [0], hostInfo->h_length);

    status = connect (maryInfoSocket, (struct sockaddr*) &maryServer, sizeof (maryServer));

    if (status != 0)
    {
      return -2;
    }
  }

  // send request to the Mary server
  if (send (maryInfoSocket, query.c_str (), query.size (), 0) == -1)
  {
    return -2;
  }


  // receive the request id
  char id [32] = "";
  if (recv (maryInfoSocket, id, 32, 0) == -1)
  {
    return -2;
  }

  //cout << "Read id: " << id << endl;

  // create a tcp connection to the mary server
  maryDataSocket = socket (AF_INET, SOCK_STREAM, 0);

  // verify that the socket could be opened successfully
  if (maryDataSocket == -1)
  {
    return -2;
  }
  else
  // autoflush stdout, bind and connect
  {
    maryClient.sin_family = AF_INET;
    maryClient.sin_port = htons (0);
    maryClient.sin_addr.s_addr = INADDR_ANY;

    int status = bind (maryDataSocket, (struct sockaddr*) &maryClient, sizeof (maryClient));

    if (status != 0)
    {
      return -2;
    }

    maryServer.sin_family = AF_INET;
    maryServer.sin_port = htons (server_port);
    memcpy ((char*) &maryServer.sin_addr.s_addr, hostInfo->h_addr_list [0], hostInfo->h_length);

    status = connect (maryDataSocket, (struct sockaddr*) &maryServer, sizeof (maryServer));

    if (status != 0)
    {
      return -2;
    }
  }


  // send the request id to the Mary server
  if (send (maryDataSocket, id, strlen (id), 0) == -1)
  {
    return -2;
  }

  //cout << "Sending request: " << inputText << endl;

  // send the query to the Mary server
  if (send (maryDataSocket, inputText.c_str (), inputText.size (), 0) == -1)
  {
    return -2;
  }

  if (send (maryDataSocket, "\012\015", 2, 0) == -1)
  {
    return -2;
  }


  // shutdown data socket
  shutdown (maryDataSocket, 1);


  //cout << "Reading result" << endl;

  int recv_bytes = 0;
  char data [1024];

  result.clear();

  // receive the request result
  do
  {
    recv_bytes = recv (maryDataSocket, data, sizeof(data), 0);

    if (recv_bytes == -1)
    {
      return -2;
    }
    else if (recv_bytes > 0)
    {
      result.append(data, recv_bytes);
    }
  } while (recv_bytes != 0);

  // receive the request error
  do
  {
    data [0] = '\0';

    recv_bytes = recv (maryInfoSocket, data, 1024, 0);

    if (recv_bytes == -1)
    {
      return -2;
    }
    else if (recv_bytes > 0)
    {
      cerr << endl << "Mary error code: " << data << endl;
      return -3;
    }
  } while (recv_bytes != 0);

  // close all open sockets
  close (maryInfoSocket);
  close (maryDataSocket);

  return 0;
}
