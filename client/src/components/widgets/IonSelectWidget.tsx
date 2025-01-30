import React from 'react';
import { IonSelect, IonSelectOption } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';

const IonSelectWidget: React.FC<WidgetProps> = ({
  id,
  options,
  value,
  required,
  disabled,
  readonly,
  onChange,
  placeholder,
}) => {
  const { enumOptions, enumDisabled } = options;
  const emptyValue = options.emptyValue || "";

  const handleChange = (event: CustomEvent) => {
    const newValue = event.detail.value;
    onChange(newValue === "" ? emptyValue : newValue);
  };

  return (
    <IonSelect
      id={id}
      value={value || emptyValue}
      placeholder={placeholder || "Select an option"}
      onIonChange={handleChange}
      disabled={disabled || readonly}
      interface="popover"
      className="w-full"
    >
      {!required && (
        <IonSelectOption value="">
          {placeholder || "Select an option"}
        </IonSelectOption>
      )}
      {Array.isArray(enumOptions) &&
        enumOptions.map(({ value, label }, i) => (
          <IonSelectOption
            key={i}
            value={value}
            disabled={Array.isArray(enumDisabled) && enumDisabled.indexOf(value) !== -1}
          >
            {label}
          </IonSelectOption>
        ))}
    </IonSelect>
  );
};

export default IonSelectWidget;
