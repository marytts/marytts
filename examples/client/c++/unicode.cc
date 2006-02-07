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
#include <unicode/ucnv.h>
#include <unicode/unistr.h>
#include <iostream>

#include "unicode.h"

std::string
convert_encoding (const std::string& src,
                  const std::string& from,
                  const std::string& to)
{
  UnicodeString unicode;
  std::string encoded;

  if (src.length () != 0)
  {
    UErrorCode _status = U_ZERO_ERROR;
    UConverter* _conv_from = ucnv_open (from.c_str (), &_status);

    if (U_FAILURE (_status))
    {
      std::cerr << "error: could not build " << from << " convertor in convert_encoding ()." << std::endl;
    }

    UConverter* _conv_to = ucnv_open (to.c_str (), &_status);

    if (U_FAILURE (_status))
    {
      std::cerr << "error: could not build " << to << " convertor in convert_encoding ()." << std::endl;
    }

    int32_t size = 2 * src.length ();
    UChar* unicodeBuffer = unicode.getBuffer (size);

    size = ucnv_toUChars (_conv_from, unicodeBuffer, size, src.c_str (), src.length (), &_status);
    if (U_FAILURE (_status))
    {
      std::cerr << "error: converting from << " << from << " in convert_encoding ()." << std::endl;
    }

    unicode.releaseBuffer (size);

    size = unicode.length () * ucnv_getMaxCharSize (_conv_to) + 1;
    char* encodedBuffer = new char [size];

    size = ucnv_fromUChars (_conv_to, encodedBuffer, size, unicode.getBuffer (), unicode.length (), &_status);
    if (U_FAILURE (_status))
    {
      std::cerr << "error: converting to << " << to << " in convert_encoding ()." << std::endl;
    }

    encodedBuffer [size] = '\0';
    encoded = std::string (encodedBuffer);
  }

  return encoded;
}
