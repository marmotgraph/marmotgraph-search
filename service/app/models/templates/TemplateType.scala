/*
 *   Copyright (c) 2020, EPFL/Human Brain Project PCO
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package models.templates

import play.api.mvc.PathBindable

sealed trait TemplateType {
  def apiName: String
}

object TemplateType {

  def orderedList(): List[TemplateType] = {
    List(Dataset, Person, ModelInstance, SoftwareProject, Project, Sample, Subject)
  }

  def apply(s: String): TemplateType = s.toUpperCase match {
    case "DATASET"         => Dataset
    case "PERSON"          => Person
    case "PROJECT"         => Project
    case "MODELINSTANCE"   => ModelInstance
    case "SOFTWAREPROJECT" => SoftwareProject
    case "SAMPLE"          => Sample
    case "SUBJECT"         => Subject
  }

  def toSchema(templateType: TemplateType): List[String] = templateType match {
    case Dataset         => List("minds/core/dataset/v1.0.0")
    case Person          => List("minds/core/person/v1.0.0", "uniminds/core/person/v1.0.0")
    case Project         => List("minds/core/placomponent/v1.0.0")
    case ModelInstance   => List("uniminds/core/modelinstance/v1.0.0")
    case SoftwareProject => List("softwarecatalog/software/softwareproject/v1.0.0")
    case Sample          => List("minds/experiment/sample/v1.0.0")
    case Subject         => List("minds/experiment/subject/v1.0.0")
  }

  def fromSchema(schema: String): Option[TemplateType] = schema match {
    case "minds/core/dataset/v1.0.0"                       => Some(Dataset)
    case "minds/core/person/v1.0.0"                        => Some(Person)
    case "uniminds/core/person/v1.0.0"                     => Some(Person)
    case "minds/core/placomponent/v1.0.0"                  => Some(Project)
    case "uniminds/core/modelinstance/v1.0.0"              => Some(ModelInstance)
    case "softwarecatalog/software/softwareproject/v1.0.0" => Some(SoftwareProject)
    case "minds/experiment/sample/v1.0.0"                  => Some(Sample)
    case "minds/experiment/subject/v1.0.0"                 => Some(Subject)
    case _                                                 => None
  }

  implicit def pathBinder(implicit stringBinder: PathBindable[String]): PathBindable[TemplateType] =
    new PathBindable[TemplateType] {
      override def bind(key: String, value: String): Either[String, TemplateType] = {
        for {
          str <- stringBinder.bind(key, value).right
        } yield TemplateType(str)
      }

      override def unbind(key: String, templateType: TemplateType): String = {
        templateType.toString
      }
    }
}

case object Dataset extends TemplateType {
  override def apiName: String = "Dataset"
}

case object Person extends TemplateType {
  override def apiName: String = "Contributor"
}

case object Project extends TemplateType {
  override def apiName: String = "Project"
}

case object ModelInstance extends TemplateType {
  override def apiName: String = "Model"
}

case object SoftwareProject extends TemplateType {
  override def apiName: String = "Software"
}
case object Sample extends TemplateType {
  override def apiName: String = "Sample"
}
case object Subject extends TemplateType {
  override def apiName: String = "Subject"
}