import React from 'react';
import { IonItem, IonLabel, IonListHeader, IonNote, IonTitle, IonToolbar } from '@ionic/react';
import { FieldTemplateProps } from '@rjsf/utils';

const IonFieldTemplate: React.FC<FieldTemplateProps> = ({
  id,
  onChange,onDropPropertyClick,registry,schema,onKeyChange,
  label,
  formData,
  children,
  description,
  help,
  errors,
  hidden,
  required,
  displayLabel,
  disabled,
  readonly
}) => {
  if (hidden) {
    return <div className="hidden">{children}</div>;
  }
  if(id==='root'){
    return <> {children}</>
  }  
  return (
    <>
        {displayLabel && label && (
          <IonTitle color={typeof formData!='undefined'?"success":"primary"} >
            {label}
            {required && typeof formData=='undefined'&&' *'}
          </IonTitle>
        )}
        {schema.type!=='object'&&<IonNote>{description}</IonNote>}
        
        <IonToolbar>
        {children}
        </IonToolbar>
        {errors}
        {help && <IonNote className="ion-padding-top ion-text-small">{help}</IonNote>}
    </>
  );
};

export default IonFieldTemplate;