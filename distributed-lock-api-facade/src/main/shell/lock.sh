#!/bin/sh

export appName=$appName

export cmd='lock'
export lockKey=${lockKey}
export timeout=${timeout}

basepath=$(cd `dirname $0`;pwd)

${basepath}/api.sh