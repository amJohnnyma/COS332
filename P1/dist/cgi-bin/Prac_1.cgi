#!/bin/bash
echo "Content-type: text/html"
echo "Cache-Control: no-cache, no-store, must-revalidate"
echo "Pragma: no-cache"
echo "Expires: 0"
echo ""

java -cp /usr/lib/cgi-bin/classes Prac_1_Home

#Note that you should consider the possibility that the browser may cache a
#web page, and that your newly generated numbers are not displayed; the browser
#just sticks to one set of numbers. Insert an appropriate meta-tag to ensure that this
#does not happen
#
# https://rocketvalidator.com/html-validation/bad-value-cache-control-for-attribute-http-equiv-on-element-meta
#
# Since it is not valid it is configured server side
#
#
#
#
