import React from 'react';
import { IonCheckbox, IonItem, IonLabel } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';

const IonCheckboxWidget: React.FC<WidgetProps> = ({
  id,
  value,
  disabled,
  readonly,
  onChange,
  label,
  schema,
}) => {
  const handleChange = (event: CustomEvent) => {
    const checked = event.detail.checked;
    onChange(checked);
  };

  return (
    <IonItem lines="none" className="ion-no-padding">
      <IonCheckbox
        id={id}
        checked={value || false}
        onIonChange={handleChange}
        disabled={disabled || readonly}
        labelPlacement="end"
        justify="start"
      >
        <IonLabel className="ion-text-wrap">
          {label || schema.title}
        </IonLabel>
      </IonCheckbox>
    </IonItem>
  );
};

export default IonCheckboxWidget;
