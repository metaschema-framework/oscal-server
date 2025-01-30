import React from 'react';
import { IonTextarea } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';

const IonTextAreaWidget: React.FC<WidgetProps> = ({
  id,
  placeholder,
  value,
  required,
  disabled,
  readonly,
  onChange,
  options = {},
}) => {
  const {
    rows = 4,
    maxLength,
    autoGrow = true,
  } = options as {
    rows?: number;
    maxLength?: number;
    autoGrow?: boolean;
  };

  const handleChange = (event: CustomEvent) => {
    const newValue = event.detail.value;
    onChange(newValue || undefined);
  };

  return (
    <IonTextarea
      id={id}
      placeholder={placeholder}
      value={value || ''}
      onIonChange={handleChange}
      disabled={disabled || readonly}
      rows={rows}
      maxlength={maxLength}
      autoGrow={autoGrow}
      className="w-full"
      fill="solid"
      counter={!!maxLength}
    />
  );
};

export default IonTextAreaWidget;
