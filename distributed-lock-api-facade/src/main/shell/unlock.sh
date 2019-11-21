#!/bin/sh

export appName=$appName

export cmd='unlock'
export lockKey=${lockKey}
export timeout=''

basepath=$(cd `dirname $0`;pwd)

${basepath}/api.sh