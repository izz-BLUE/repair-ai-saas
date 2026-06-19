import React, { createContext, useContext } from 'react';
import type { PortalConfig } from '../api/portal';

const defaultConfig: PortalConfig = {
  name: '',
  portalTitle: '智能服务平台',
  portalDescription: '',
  contactPhone: '',
  logoUrl: '',
  themeColor: '#0d9488',
  portalEnabled: true,
};

const PortalConfigContext = createContext<PortalConfig>(defaultConfig);

export const PortalConfigProvider: React.FC<{
  config: PortalConfig;
  children: React.ReactNode;
}> = ({ config, children }) => {
  return (
    <PortalConfigContext.Provider value={config}>
      {children}
    </PortalConfigContext.Provider>
  );
};

/** 获取门户配置，必须在 PortalConfigProvider 内使用 */
export const usePortalConfig = (): PortalConfig => {
  return useContext(PortalConfigContext);
};

/** 获取主题色（带 fallback） */
export const useThemeColor = (): string => {
  const config = useContext(PortalConfigContext);
  return config.themeColor || '#0d9488';
};

export default PortalConfigContext;
