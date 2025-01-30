import React from 'react';
import { IonDatetime } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';

const IonDateTimeWidget: React.FC<WidgetProps> = ({
  id,
  value,
  required,
  disabled,
  readonly,
  onChange,
  options,
}) => {
  const {
    format = 'YYYY-MM-DD',
    presentation = 'date',
    min = '',
    max = '',
  } = options as {
    format?: string;
    presentation?: string;
    min?: string;
    max?: string;
  };

  const handleChange = (event: CustomEvent) => {
    const newValue = event.detail.value;
    onChange(newValue || undefined);
  };

  return (
    <IonDatetime
      id={id}
      value={value || ''}
      onIonChange={handleChange}
      disabled={disabled || readonly}
      presentation={presentation as 'date' | 'date-time' | 'time' | 'month' | 'year'}
      min={min}
      max={max}
      className="w-full"
      style={{ border: '1px solid var(--ion-color-medium)' }}
      preferWheel={true}
    />
  );
};

export default IonDateTimeWidget;
