#!/bin/bash
set -eo pipefail
ARTIFACT_BUCKET=$(cat bucket-name.txt)
aws s3 cp images/sample-s3-java.png s3://$ARTIFACT_BUCKET/inbound/sample-s3-java.png
TEMPLATE=template.yml

gradle build -i -x test

aws cloudformation package --template-file $TEMPLATE --s3-bucket $ARTIFACT_BUCKET --output-template-file out.yml
aws cloudformation deploy --template-file out.yml --stack-name s3-java --capabilities CAPABILITY_NAMED_IAM
