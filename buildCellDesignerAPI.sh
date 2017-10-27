#*******************************************************************************
# Copyright 2016 Kaito Ii
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#*******************************************************************************
#!/bin/sh
xjc -J-Duser.language=en -d src/main/java schema/CellDesigner.xsd schema/sbml-level-2-v4-wo-annotation.xsd schema/sbml-mathml.xsd schema/sbmlCellDesignerExtension_v4_2.xsd
