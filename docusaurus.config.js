/** @type {import('@docusaurus/types').DocusaurusConfig} */
module.exports = {
  title: 'Mega Manipulator (m²)',
  tagline: 'Change everything',
  url: 'https://github.com/jensim/mega-manipulator',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'https://raw.githubusercontent.com/jensim/mega-manipulator/main/src/main/resources/META-INF/pluginIcon.svg',
  organizationName: 'jensim', // Usually your GitHub org/user name.
  projectName: 'mega-manipulator', // Usually your repo name.
  themeConfig: {
    navbar: {
      title: 'Mega Manipilator',
      /*logo: {
        alt: 'My Site Logo',
        src: 'img/logo.svg',
      },*/
      items: [
        {
          type: 'doc',
          docId: 'intro',
          position: 'left',
          label: 'Tutorial',
        },
        {
          href: 'https://github.com/jensim/mega-manipulator/',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    /*
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Tutorial',
              to: '/docs/intro',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Stack Overflow',
              href: 'https://stackoverflow.com/questions/tagged/docusaurus',
            },
            {
              label: 'Discord',
              href: 'https://discordapp.com/invite/docusaurus',
            },
            {
              label: 'Twitter',
              href: 'https://twitter.com/docusaurus',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Blog',
              to: '/blog',
            },
            {
              label: 'GitHub',
              href: 'https://github.com/facebook/docusaurus',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} My Project, Inc. Built with Docusaurus.`,
    },
    */
  },
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          editUrl:
            'https://github.com/jensim/mega-manipulator/tree/docs',
        },
        /*
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          editUrl:
            'https://github.com/facebook/docusaurus/edit/master/website/blog/',
        },
        */
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};
