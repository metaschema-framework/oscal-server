import React from 'react';
import { IonInput } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';

const IonTextWidget: React.FC<WidgetProps> = ({
  id,
  value,
  required,
  disabled,
  readonly,
  onChange,
  onBlur,
  onFocus,
  autofocus,
  placeholder
}) => {
  const handleChange = (event: Event) => {
    const value = (event.target as HTMLIonInputElement).value?.toString() || '';
    onChange(value === '' ? undefined : value);
  };

  return (
    <IonInput
      id={id}
      color='primary'
      value={value || ''}
      required={required}
      disabled={disabled || readonly}
      placeholder={placeholder}
      onIonChange={handleChange}
      onIonBlur={(e) => onBlur(id, e.target.value?.toString())}
      onIonFocus={(e) => onFocus(id, e.target.value?.toString())}
      autofocus={autofocus}
      className="ion-margin-vertical"
    />
  );
};

export default IonTextWidget;