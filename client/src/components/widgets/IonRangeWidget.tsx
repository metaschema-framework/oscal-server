import React from 'react';
import { IonRange, IonLabel } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';

const IonRangeWidget: React.FC<WidgetProps> = ({
  id,
  value,
  required,
  disabled,
  readonly,
  onChange,
  schema,
  options = {},
  label,
}) => {
  const {
    step = 1,
    min = schema.minimum ?? 0,
    max = schema.maximum ?? 100,
    pin = true,
    snaps = false,
    ticks = false,
  } = options as {
    step?: number;
    min?: number;
    max?: number;
    pin?: boolean;
    snaps?: boolean;
    ticks?: boolean;
  };

  const handleChange = (event: CustomEvent) => {
    const newValue = event.detail.value;
    onChange(newValue);
  };

  return (
    <div className="ion-padding-vertical">
      {label && (
        <IonLabel color="primary" className="ion-padding-bottom">
          {label}
          {required && ' *'}
        </IonLabel>
      )}
      <IonRange
        id={id}
        value={value ?? min}
        onIonChange={handleChange}
        disabled={disabled || readonly}
        min={min}
        max={max}
        step={step}
        pin={pin}
        snaps={snaps}
        ticks={ticks}
        labelPlacement="start"
      />
    </div>
  );
};

export default IonRangeWidget;
