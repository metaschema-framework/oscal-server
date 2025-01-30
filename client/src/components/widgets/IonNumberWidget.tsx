import React from 'react';
import { IonInput } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';

const IonNumberWidget: React.FC<WidgetProps> = ({
  id,
  placeholder,
  value,
  required,
  disabled,
  readonly,
  onChange,
  schema,
  options = {},
}) => {
  const {
    step,
    min,
    max,
  } = options as {
    step?: number;
    min?: number;
    max?: number;
  };

  const handleChange = (event: CustomEvent) => {
    const newValue = event.detail.value;
    // Convert empty string to undefined, string number to number
    const numberValue = newValue === '' ? undefined : Number(newValue);
    onChange(numberValue);
  };

  return (
    <IonInput
      id={id}
      type="number"
      placeholder={placeholder}
      value={value?.toString()}
      onIonChange={handleChange}
      disabled={disabled || readonly}
      min={(min ?? schema.minimum)?.toString()}
      max={(max ?? schema.maximum)?.toString()}
      step={step?.toString()}
      className="w-full"
      fill="solid"
    />
  );
};

export default IonNumberWidget;
