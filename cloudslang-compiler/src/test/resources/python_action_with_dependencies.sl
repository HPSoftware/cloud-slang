#   (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
#   All rights reserved. This program and the accompanying materials
#   are made available under the terms of the Apache License v2.0 which accompany this distribution.
#
#   The Apache License is available at
#   http://www.apache.org/licenses/LICENSE-2.0

namespace: user.ops

operation:
  name: python_action_with_dependencies
  python_action:
    dependencies:
      - 'a==v1'
      - 'a == v2'
    script: 'pass'
  results:
    - SUCCESS
