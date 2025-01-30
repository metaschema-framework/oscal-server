import {
  IonAccordion,
  IonAccordionGroup,
  IonButtons,
  IonItem,
  IonLabel,
  IonList,
  IonNote,
} from "@ionic/react";
import { ObjectFieldTemplateProps } from "@rjsf/utils";
import React from "react";

const IonObjectFieldTemplate: React.FC<ObjectFieldTemplateProps> = ({
  title,
  description,
  properties,
  required,
  disabled,
  readonly,
  uiSchema,
  schema,
  idSchema,
}) => {

  if (!title || idSchema.$id === "root") {
    // If no title, render properties directly with optional fields in accordion
    const requiredFields = properties.filter(prop => 
      schema.required && schema.required.includes(prop.name)
    );
    const optionalFields = properties.filter(prop => 
      !schema.required || !schema.required.includes(prop.name)
    );

    return (
      <div className="ion-padding-vertical">
        {requiredFields.map((prop) => (
          <IonItem
            key={prop.name}
            disabled={disabled || prop.disabled}
            lines="full"
          >
            <IonLabel position="floating">{prop.content.props.label}</IonLabel>
            {prop.content}
          </IonItem>
        ))}

        {optionalFields.length > 0 && (
          <IonAccordionGroup expand="inset">
            <IonAccordion value="optional-fields">
              <IonItem slot="header" color="light" lines="none">
                <IonLabel>Optional Fields</IonLabel>
              </IonItem>
              
              <IonList slot="content">
                {optionalFields.map((prop) => (
                  <IonItem
                    key={prop.name}
                    disabled={disabled || prop.disabled}
                    lines="full"
                  >
                    <IonLabel position="floating">{prop.content.props.label}</IonLabel>
                    {prop.content}
                  </IonItem>
                ))}
              </IonList>
            </IonAccordion>
          </IonAccordionGroup>
        )}
      </div>
    );
  }

  return (
    <IonAccordionGroup
      expand={"inset"}
    >
      <IonAccordion value={idSchema.$id}>
        <IonItem
          slot="header"
          color="light"
          lines="none"
        >
          <IonButtons slot="start">
            <IonLabel>{title}</IonLabel>
          </IonButtons>
          <IonButtons >
          <IonNote className="ion-text-wrap">
            {description && <p>{description}</p>}
          </IonNote>
          </IonButtons>
        </IonItem>

        <IonList slot="content">
          {properties.map((prop) => (
            <IonItem
              key={prop.name}
              disabled={disabled || prop.disabled}
              lines="full"
            >
              <IonLabel position="stacked">
                <IonButtons slot="end">{prop.content.props.label}</IonButtons>
              </IonLabel>
              {prop.content}
            </IonItem>
          ))}
        </IonList>
      </IonAccordion>
    </IonAccordionGroup>
  );
};

export default IonObjectFieldTemplate;
