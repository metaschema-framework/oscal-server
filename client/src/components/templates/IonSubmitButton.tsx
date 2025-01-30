import React from 'react';
import { IonButton, IonIcon } from '@ionic/react';
import { SubmitButtonProps } from '@rjsf/utils';
import { checkmarkCircle, cloudUpload, enter, saveOutline } from 'ionicons/icons';

interface ButtonOptions {
  color?: 'primary' | 'secondary' | 'tertiary' | 'success' | 'warning' | 'danger' | 'light' | 'medium' | 'dark';
  fill?: 'clear' | 'outline' | 'solid';
  size?: 'small' | 'default' | 'large';
  expand?: 'block' | 'full';
  shape?: 'round';
  strong?: boolean;
  icon?: boolean;
  submitText?: string;
}

const IonSubmitButton: React.FC<SubmitButtonProps> = ({
  uiSchema
}) => {
  const options: ButtonOptions = (uiSchema?.['ui:submitButtonOptions'] || {}) as ButtonOptions;
  
  const {
    color = 'primary',
    fill = 'solid',
    size = 'default',
    expand = 'block',
    shape,
    strong = false,
    icon = true,
    submitText = 'Submit'
  } = options;

  return (
    <IonButton 
      type='submit'
      color={color}
      fill={fill}
      size={size}
      expand={expand}
      shape={shape}
      strong={strong}
      className="ion-margin-vertical"
    >
      {icon && <IonIcon slot="start" icon={saveOutline} />}
      {submitText}
    </IonButton>
  );
};

export default IonSubmitButton;
