import { IonCard, IonSpinner, IonText } from "@ionic/react";
import Form from "@rjsf/core";
import validator from "@rjsf/validator-ajv8";
import React, { useEffect, useState } from "react";
import oscalSchema from "../schema.json";
import "./form.css";
import IonFieldTemplate from "./templates/IonFieldTemplate";
import IonObjectFieldTemplate from "./templates/IonObjectFieldTemplate";
import IonTextWidget from "./widgets/IonTextWidget";
import IonTitleFieldTemplate from "./templates/IonTitleFieldTemplate";
import IonUuidWidget from "./widgets/IonUuidWidget";
import IonSelectWidget from "./widgets/IonSelectWidget";
import IonOrganizationAffiliationWidget from "./widgets/IonOrganizationAffiliationWidget";
import IonImportProfileWidget from "./widgets/IonImportProfileWidget";
import IonDocumentResourceWidget from "./widgets/IonDocumentResourceWidget";
import IonResponsiblePartyWidget from "./widgets/IonResponsiblePartyWidget";
import IonPartySelectWidget from "./widgets/IonPartySelectWidget";
import IonArrayFieldTemplate from "./templates/IonArrayFieldTemplate";
import IonSubmitButton from "./templates/IonSubmitButton";
import IonErrorListTemplate from "./templates/IonErrorListTemplate";
import IonBaseInputTemplate from "./templates/IonBaseInputTemplate";
import IonAddButton from "./templates/IonAddButton";
import {
  IonMoveUpButton,
  IonMoveDownButton,
  IonCopyButton,
  IonRemoveButton,
} from "./templates/IonArrayButtons";
import IonArrayFieldItemTemplate from "./templates/IonArrayFieldItemTemplate";
import { OscalDefinition } from "../types";

interface OscalFormProps {
  type: OscalDefinition;
  initialData?: Record<string, unknown>;
  onSubmit?: (data: Record<string, unknown>) => void;
}

const uiSchema = {
  "ui:submitButtonOptions": {
    submitText: "Save Changes",
    color: "success",
    fill: "solid",
    size: "large",
    expand: "block",
    strong: true,
    icon: true,
  },
  "ui:options": {
    addable: true,
    orderable: true,
    removable: true,
    inline: false,
  },
  uuid: {
    "ui:widget": "ion-uuid",
  },
  "member-of-organizations": {
    "ui:widget": "ion-organization-affiliation",
  },
  "import-profile": {
    "ui:title": "Control Baseline Profile",
    "ui:description": "Select a predefined baseline or import a custom profile",
    "ui:options": {
      "modal": false
    },
    href: {
      "ui:widget": "ion-import-profile"
    }
  },
  documentReference: {
    "ui:widget": "ion-document-resource",
  },
  "responsible-party": {
    "ui:widget": "ion-responsible-party",
  },
  "responsible-parties": {
    "ui:widget": "ion-responsible-party",
  },
  "party-uuids": {
    "ui:widget": "ion-party-select",
  },
  riskManagementRoles: {
    "ui:widget": "ion-responsible-party",
    "ui:options": {
      roleId: "risk-manager",
      partyType: "organization"
    },
    "ui:title": "Risk Management Roles",
    "ui:description": "Select organizations responsible for risk management",
  },
  riskAssumptions: {
    "ui:widget": "textarea",
    "ui:title": "Risk Assumptions",
    "ui:description": "Document key assumptions about risk management",
  },
  riskConstraints: {
    "ui:widget": "textarea",
    "ui:title": "Risk Constraints",
    "ui:description": "Document constraints affecting risk management",
  },
  riskTolerance: {
    "ui:widget": "textarea",
    "ui:title": "Risk Tolerance",
    "ui:description": "Define organizational risk tolerance levels",
  },
  controlImplementation: {
    "ui:widget": "textarea",
    "ui:title": "PM-9 Control Implementation",
    "ui:description": "Describe how PM-9 (Risk Management Strategy) is implemented",
  },
  "*": {
    "ui:options": {
      modal: true,
    },
  },
};

const widgets = {
  "ion-text": IonTextWidget,
  "ion-uuid": IonUuidWidget,
  "ion-select": IonSelectWidget,
  "ion-organization-affiliation": IonOrganizationAffiliationWidget,
  "ion-import-profile": IonImportProfileWidget,
  "ion-document-resource": IonDocumentResourceWidget,
  "ion-responsible-party": IonResponsiblePartyWidget,
  "ion-party-select": IonPartySelectWidget,
  SelectWidget: IonSelectWidget,
};

const fields = {
  "ion-text": IonTextWidget,
  "ion-uuid": IonUuidWidget,
  "ion-select": IonSelectWidget,
};

const OscalForm: React.FC<OscalFormProps> = ({ type, initialData, onSubmit }) => {
  const [schema, setSchema] = useState<Record<string, unknown> | null>(null);
  const [formData, setFormData] = useState<Record<string, unknown>>(initialData || {});

  useEffect(() => {
    const loadSchema = () => {
      const definitions = oscalSchema.definitions as Record<string, unknown>;
      const formSchema = {
        type: "object",
        $ref: `#/definitions/${type}`,
        definitions,
      };

      if (formSchema) {
        setSchema(formSchema);
        console.log("SETTING SCHEMA");
        setFormData(initialData || {});
      } else {
        console.error("No valid schema found for type:", type);
        setSchema(null);
      }
    };

    loadSchema();
  }, [type, initialData]);

  if (!schema) {
    return (
      <div className="ion-text-center">
        <IonSpinner name="circular" />
        <IonText>
          <p>Loading schema...</p>
        </IonText>
      </div>
    );
  }

  const handleSubmit = ({
    formData,
  }: {
    formData: Record<string, unknown>;
  }) => {
    onSubmit?.(formData);
    setTimeout(()=>{
      setFormData({});
    },100)
  };

  return (
    <IonCard>
      <Form
        schema={schema}
        uiSchema={uiSchema}
        validator={validator}
        formData={formData}
        onChange={({ formData }) => {
          setFormData(formData as any);
        }}
        onSubmit={handleSubmit as any}
        templates={{
          ArrayFieldItemTemplate:IonArrayFieldItemTemplate,
          ErrorListTemplate: IonErrorListTemplate,
          FieldTemplate: IonFieldTemplate,
          ObjectFieldTemplate: IonObjectFieldTemplate,
          ArrayFieldTemplate: IonArrayFieldTemplate,
          BaseInputTemplate: IonBaseInputTemplate,
          TitleFieldTemplate: IonTitleFieldTemplate,
          ButtonTemplates: {
            SubmitButton: IonSubmitButton,
            AddButton: IonAddButton,
            MoveUpButton: IonMoveUpButton,
            MoveDownButton: IonMoveDownButton,
            CopyButton: IonCopyButton,
            RemoveButton: IonRemoveButton,
          },
        }}
        widgets={widgets}
      />
    </IonCard>
  );
};

export default OscalForm;
